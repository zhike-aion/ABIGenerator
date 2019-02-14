# ABIGenerator

A tool to extract callable methods and to modify bytecodes of classes in a dapp jar.

## Example Usage

```
cd ABIGenerator 
ABIGenerator$ javac -cp "lib/*" src/org/aion/avm/core/abicompiler/*.java -d out/ABIGenerator
ABIGenerator$ java -cp lib/*:out/ABIGenerator/ org.aion.avm.core.abicompiler.ABICompiler test/resources/dapp/dapp.jar 
```

After running the upper commands, the method descriptions would be printed out, and the classes namely *.class would be saved in the current folder. 
