package jviz.uci.edu;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;

public class UCIJSObject{
	public JSObject obj = null;
	
	public UCIJSObject(JSObject obj){
		super();
		this.obj = obj;
	}
	
	

	public UCIJSObject  attr(Object... args){
		obj.call("attr", args);
		return this;		
	}
	
	public UCIJSObject  style(Object... args){
		obj.call("style", args);
		return this;		
	}
	public UCIJSObject  text(Object... args){
		obj.call("text", args);
		return this;		
	}
	
	public UCIJSObject  append(Object... args){
		return new UCIJSObject((JSObject)obj.call("append", args));
	}
	
	public UCIJSObject  select(Object... args){
		return new UCIJSObject((JSObject) obj.call("select", args));		
	}
	

//	@Override
//	public Object call(String methodName, Object... args) throws JSException {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Object eval(String s) throws JSException {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Object getMember(String name) throws JSException {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public void setMember(String name, Object value) throws JSException {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void removeMember(String name) throws JSException {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public Object getSlot(int index) throws JSException {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public void setSlot(int index, Object value) throws JSException {
//		// TODO Auto-generated method stub
//		
//	}

}
