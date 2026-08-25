package io.github.wooongchan.requestflow.capture;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * 리플렉션 Type을 "List&lt;Order&gt;" 같이 사람이 읽기 좋은 짧은 형태로 변환한다.
 */
public final class TypeNames {

    private TypeNames() {
    }

    public static String format(Type type) {
        if (type == null) {
            return "Object";
        }
        if (type instanceof Class<?>) {
            return simpleName((Class<?>) type);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            StringBuilder sb = new StringBuilder(format(parameterizedType.getRawType()));
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            if (typeArgs.length > 0) {
                sb.append('<');
                for (int i = 0; i < typeArgs.length; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(format(typeArgs[i]));
                }
                sb.append('>');
            }
            return sb.toString();
        }
        if (type instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            return upperBounds.length > 0 ? format(upperBounds[0]) : "Object";
        }
        return type.getTypeName();
    }

    private static String simpleName(Class<?> clazz) {
        if (clazz.isArray()) {
            return simpleName(clazz.getComponentType()) + "[]";
        }
        return clazz.getSimpleName();
    }
}
