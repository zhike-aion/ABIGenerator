package org.aion.abigenerator;

public class CallableMismatchNonStaticException extends RuntimeException {
    public CallableMismatchNonStaticException(String methodName) {
        super(String.format("Annotation 'Callable' mismatches non-static access modifiers for method %s!", methodName));
    }
}
