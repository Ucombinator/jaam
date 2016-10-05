import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.ImageIcon;
//import javax.swing.JLabel;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import javax.swing.text.BadLocationException;
import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;


public class SearchField extends JPanel implements DocumentListener
{
    private static enum Mode
    {
        READY, INSERT, COMPLETE
    };
//    private JLabel completed;
    private JTextField current;
    private JButton searchButton, helpButton;
    private String[] viable = {" ","(",")","&","|","~","=","<","<=",">",">=","id", "in", "method","command","contains"};
//    private int nViable = 14;
    private Mode mode = Mode.READY;
    private static final String COMMIT_ACTION = "commit";
    
    public static boolean focused = false;
    
	public SearchField()
	{
//        this.setLayout(new GridLayout(1,2));
        this.setLayout(new BorderLayout());
        
//        this.completed = new JLabel("");
//        this.add(this.completed);

        this.current = new JTextField("");
        this.current.getDocument().addDocumentListener(this);
        this.current.addFocusListener
        (
            new FocusListener()
            {
                public void focusGained(FocusEvent e)
                {
                    System.out.println("focus gained");
                    SearchField.focused = true;
                }
            
                public void focusLost(FocusEvent e)
                {
                    System.out.println("focus lost");
                    SearchField.focused = false;
                }
            }
        );
        this.current.addActionListener
        (
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if(mode == Mode.COMPLETE)
                    {
                        mode = Mode.INSERT;
                        int pos = current.getSelectionEnd();
                        StringBuffer sb = new StringBuffer(current.getText());
                        sb.insert(pos, " ");
                        current.setText(sb.toString());
                        current.setCaretPosition(pos + 1);
                        mode = Mode.READY;
                    }
                    else
                    {
                        
                    }
                }
            }
        );
        this.add(this.current, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1,2));
        this.add(buttonPanel, BorderLayout.EAST);
        
        
//        ImageIcon searchImage = new ImageIcon(Parameters.pwd+"src/visualizer/images/"+"search.png");

        this.searchButton = new JButton("Find");
        this.searchButton.addActionListener
        (
            new ActionListener()
            {
                public void actionPerformed(ActionEvent ev)
                {
                    
                }
            }
        );
        buttonPanel.add(this.searchButton);
        
        this.helpButton = new JButton("Help");
        this.helpButton.addActionListener
        (
            new ActionListener()
            {
                public void actionPerformed(ActionEvent ev)
                {
                
                }
            }
        );
        buttonPanel.add(this.helpButton);
        
        

//        this.current.setFocusTraversalKeysEnabled(false);
        
//        this.current.getInputMap().put(KeyStroke.getKeyStroke("TAB"), COMMIT_ACTION);
//        this.current.getInputMap().put(KeyStroke.getKeyStroke(new Character(' '), InputEvent.SHIFT_DOWN_MASK), COMMIT_ACTION);
//        this.current.getActionMap().put(COMMIT_ACTION, new CommitAction());
        
        this.setVisible(true);

    }
    
    public void changedUpdate(DocumentEvent ev)
    {
//        System.out.println("changed");
    }
    
    public void removeUpdate(DocumentEvent ev)
    {
//        System.out.println("removed");
    }
    
    public void insertUpdate(DocumentEvent ev)
    {
        if(mode == Mode.INSERT)
            return;
        int pos = ev.getOffset();
        String content = null;
        try
        {
            content = this.current.getText(0, pos + 1);
        }
        catch (BadLocationException e)
        {
            e.printStackTrace();
        }
        
        
        // Find where the word starts
        int w = this.getWordStart(content);
        
        String prefix = content.substring(w).toLowerCase();
        if(prefix.length()<1)
            return;
        String match = this.findCompletion(prefix);
        if(match!=null)
        {
            String completion = match.substring(pos - w + 1);
            SwingUtilities.invokeLater(new CompletionTask(completion, pos + 1));
        }
        
    }
    
    
    
    public String findCompletion(String prefix)
    {
        for(int i=0; i<viable.length; i++)
        {
            if(viable[i].startsWith(prefix))
                return viable[i];
        }
        return null;
    }
    
    
    public int getWordStart(String text)
    {
        int max = 0;
        for(int i=0; i<viable.length; i++)
        {
            int pos = text.lastIndexOf(this.viable[i]);
            if(pos>=0)
                pos = pos + this.viable[i].length();
            if(pos>max)
                max = pos;
        }
        return max;
    }
    



    private class CompletionTask implements Runnable
    {
        private String completion;
        private int position;
        
        CompletionTask(String completion, int position)
        {
            this.completion = completion;
            this.position = position;
        }
        
        public void run()
        {
            mode = Mode.INSERT;
            StringBuffer sb = new StringBuffer(current.getText());
            sb.insert(position, completion);
            current.setText(sb.toString());
            current.setCaretPosition(position + completion.length());
            current.moveCaretPosition(position);
            mode = Mode.COMPLETE;
        }
    }
    
}


