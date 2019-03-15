## The new location of this project is at https://github.com/aionnetwork/AVM , in module of AVM/org.aion.avm.tooling .

# ABIGenerator

A tool to extract callable methods and to modify bytecodes of classes in a dapp jar.

## Example Usage

```
cd ABIGenerator 
ABIGenerator$ javac -cp "lib/*" src/org/aion/abigenerator/*.java -d out/ABIGenerator
ABIGenerator$ java -cp lib/*:out/ABIGenerator/ org.aion.abigenerator.ABICompiler test/dapp.jar 
```

After running the upper commands, the method descriptions would be printed out, and the classes namely *.class would be saved in the current folder. 
