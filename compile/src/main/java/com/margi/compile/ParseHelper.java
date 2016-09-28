package com.margi.compile;

import com.margi.annotation.InjectView;
import com.squareup.javapoet.TypeName;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;


/**
 * Created by maji on 2016/9/19.
 * 注解解析绑定帮助类
 */
public final class ParseHelper {

    private static final String INJECTING_CLASS_SUFFIX = "$$ViewInjector";
    private static final String LIST_TYPE = List.class.getCanonicalName();
    private static final String ITERABLE_TYPE = "java.lang.Iterable<?>";
    static final String VIEW_TYPE = "android.view.View";
    static final int NO_ID = -1;


    private ParseHelper() {
        throw new AssertionError("No instances.");
    }


    /**
     * 解析 View 资源
     *
     * @param element        使用注解的元素
     * @param targetClassMap 映射表
     * @param elementUtils   元素工具类
     */
    public static void parseViewBind(Element element, Map<TypeElement, InjectClass> targetClassMap,
                                     Set<TypeElement> erasedTargetNames,
                                     Elements elementUtils, Types typeUtils, Messager messager) {
        TypeMirror elementType = element.asType();
        // 判断是一个 View 还是列表
        if (_isSubtypeOfType(elementType, ITERABLE_TYPE)) {
            _error(messager, element, "@%s must be a List or array. (%s.%s)", InjectView.class.getSimpleName(),
                    ((TypeElement) element.getEnclosingElement()).getQualifiedName(),
                    element.getSimpleName());
        } else {
            _parseBindOne(element, targetClassMap, erasedTargetNames, elementUtils, messager);
            _log(messager,element,"parsebindOne getEnclosingElement getQualifiedName = %s  getSimpleName = %s",((TypeElement) element.getEnclosingElement()).getQualifiedName(),element.getSimpleName());
        }
    }

    /*************************************************************************/

    /**
     * 获取存在的 InjectClass，没有则重新生成
     *
     * @param element        使用注解的元素
     * @param targetClassMap 映射表
     * @param elementUtils   元素工具类
     * @return InjectClass
     */
    private static InjectClass _getOrCreateTargetClass(Element element, Map<TypeElement, InjectClass> targetClassMap,
                                                       Elements elementUtils, Messager messager) {
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
        InjectClass injectClass = targetClassMap.get(enclosingElement);
        // 以下以 com.butterknife.MainActivity 这个类为例
        if (injectClass == null) {
            // 获取元素的完全限定名称：com.butterknife.MainActivity
            String targetType = enclosingElement.getQualifiedName().toString();
            _log(messager,element,"_getOrCreateTargetClass targetType = %s ",targetType);
            // 获取元素所在包名：com.butterknife
            String classPackage = elementUtils.getPackageOf(enclosingElement).getQualifiedName().toString();
            _log(messager,element,"_getOrCreateTargetClass classPackage = %s ",classPackage);
            // 获取要生成的Class的名称：MainActivity$$ViewBinder
            int packageLen = classPackage.length() + 1;
            String className = targetType.substring(packageLen).replace('.', '$') + INJECTING_CLASS_SUFFIX;
            _log(messager,element,"_getOrCreateTargetClass className = %s ",className);
            // 生成Class的完全限定名称：com.butterknife.MainActivity$$ViewBinder
            String classFqcn = classPackage + "." + className;
            _log(messager,element,"_getOrCreateTargetClass classFqcn = %s ",classFqcn);
            /* 不要用下面这个来生成Class名称，内部类会出错,比如ViewHolder */
//            String className = enclosingElement.getSimpleName() + INJECTING_CLASS_SUFFIX;

            injectClass = new InjectClass(classPackage, className, targetType, classFqcn);

            targetClassMap.put(enclosingElement, injectClass);
        }
        return injectClass;
    }

    /**
     * 先通过 Types 工具对元素类型进行形式类型参数擦除，再通过字符比对进行二次擦除如果必要的话
     * 例：java.util.List<java.lang.String> -> java.util.List
     *
     * @param elementType 元素类型
     * @param typeUtils   类型工具
     * @return 类型完全限定名
     */
    private static String _doubleErasure(TypeMirror elementType, Types typeUtils) {
        String name = typeUtils.erasure(elementType).toString();
        int typeParamStart = name.indexOf('<');
        if (typeParamStart != -1) {
            name = name.substring(0, typeParamStart);
        }
        return name;
    }

    /**
     * 判断该类型是否为 otherType 的子类型
     *
     * @param typeMirror 元素类型
     * @param otherType  比对类型
     * @return
     */
    private static boolean _isSubtypeOfType(TypeMirror typeMirror, String otherType) {
        if (otherType.equals(typeMirror.toString())) {
            return true;
        }
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return false;
        }
        DeclaredType declaredType = (DeclaredType) typeMirror;
        // 判断泛型参数列表
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() > 0) {
            StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
            typeString.append('<');
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) {
                    typeString.append(',');
                }
                typeString.append('?');
            }
            typeString.append('>');
            if (typeString.toString().equals(otherType)) {
                return true;
            }
        }
        // 判断是否为类或接口类型
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return false;
        }
        // 判断父类
        TypeElement typeElement = (TypeElement) element;
        TypeMirror superType = typeElement.getSuperclass();
        if (_isSubtypeOfType(superType, otherType)) {
            return true;
        }
        // 判断接口
        for (TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (_isSubtypeOfType(interfaceType, otherType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 输出错误信息
     *
     * @param element
     * @param message
     * @param args
     */
    private static void _error(Messager messager, Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    private static void _log(Messager messager, Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        messager.printMessage(Diagnostic.Kind.NOTE, message, element);
    }

    /**
     * 解析单个View绑定
     *
     * @param element
     * @param targetClassMap
     * @param erasedTargetNames
     */
    private static void _parseBindOne(Element element, Map<TypeElement, InjectClass> targetClassMap,
                                      Set<TypeElement> erasedTargetNames, Elements elementUtils, Messager messager) {
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        TypeMirror elementType = element.asType();
        if (elementType.getKind() == TypeKind.TYPEVAR) {
            // 处理泛型，取它的上边界，例：<T extends TextView> -> TextView
            TypeVariable typeVariable = (TypeVariable) elementType;
            elementType = typeVariable.getUpperBound();
        }
        // 不是View的子类型，且不是接口类型则报错
        if (!_isSubtypeOfType(elementType, VIEW_TYPE) && !_isInterface(elementType)) {
            _error(messager, element, "@%s fields must extend from View or be an interface. (%s.%s)",
                    InjectView.class.getSimpleName(), enclosingElement.getQualifiedName(), element.getSimpleName());
            return;
        }

        // 资源ID只能有一个
        int[] ids = element.getAnnotation(InjectView.class).value();
        if (ids.length != 1) {
            _error(messager, element, "@%s for a view must only specify one ID. Found: %s. (%s.%s)",
                    InjectView.class.getSimpleName(), Arrays.toString(ids), enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            return;
        }

        // 获取或创建绑定类
        int id = ids[0];
        InjectClass injectClass = _getOrCreateTargetClass(element, targetClassMap, elementUtils, messager);
        FieldViewInjecting existViewBinding = injectClass.isExistViewBinding(id);
        if (existViewBinding != null) {
            // 存在重复使用的ID
            _error(messager, element, "Attempt to use @%s for an already bound ID %d on '%s'. (%s.%s)",
                    InjectView.class.getSimpleName(), id, existViewBinding.getName(),
                    enclosingElement.getQualifiedName(), element.getSimpleName());
            return;
        }

        String name = element.getSimpleName().toString();
        TypeName type = TypeName.get(elementType);
        // 生成资源信息
        FieldViewInjecting binding = new FieldViewInjecting(name, type, true);
        // 给BindingClass添加资源信息
        injectClass.addViewBinding(id, binding);

        erasedTargetNames.add(enclosingElement);
    }


    /**
     * 判断是否为接口
     *
     * @param typeMirror
     * @return
     */
    private static boolean _isInterface(TypeMirror typeMirror) {
        return typeMirror instanceof DeclaredType
                && ((DeclaredType) typeMirror).asElement().getKind() == ElementKind.INTERFACE;
    }

    /**
     * 检测重复ID
     */
    private static Integer _findDuplicate(int[] array) {
        Set<Integer> seenElements = new LinkedHashSet<>();

        for (int element : array) {
            if (!seenElements.add(element)) {
                return element;
            }
        }
        return null;
    }
}
