package org.aion.abigenerator;

public class CallableMismatchNonPublicException extends RuntimeException {
    public CallableMismatchNonPublicException(String methodName) {
        super(String.format("Annotation 'Callable' mismatches non-public access modifiers(protected/private) for method %s!", methodName));
    }
}
