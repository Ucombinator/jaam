Steps to get Z3 library working:

Pull latest from uCombinator github master branch.
You need to copy z3java libraries to the java library path on your machine to overcome an issue with z3 (https://github.com/Z3Prover/z3/issues/294). 
To copy the libraries:

On Mac:
Copy libz3.dylib to /usr/local/lib and libz3java.dylib to /Library/Java/Extensions on your  machine. Find libz3.dylib and libz3java.dylib files under the jaam/resources folder.
 	
On Windows:
Copy libz3.dll and libz3java.dll from jaam/resources to any read-permissioned folder on your machine. Add that folder path to your windows PATH variable. 

Now restart your terminals for the changes to take effect and go ahead to build and run jaam as usual.