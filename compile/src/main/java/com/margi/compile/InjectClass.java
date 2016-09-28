package com.margi.compile;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;

/**
 * Created by margi on 2016/8/11.
 * 绑定处理类，一个 InjectClass 对应一个要生成的类
 */
public final class InjectClass {

    private static final ClassName FINDER = ClassName.get("com.margi.core", "Finder");
    private static final ClassName VIEW_BINDER = ClassName.get("com.margi.core", "ViewInjector");
    private static final ClassName VIEW = ClassName.get("android.view", "View");
    private static final ClassName UTILS = ClassName.get("com.margi.core", "Utils");
    private static final ClassName CONTEXT = ClassName.get("android.content", "Context");
    private static final ClassName CONTEXT_COMPAT = ClassName.get("android.support.v4.content", "ContextCompat");
//
    private final List<FieldViewInjecting> viewBindings = new ArrayList<>();
    private final Map<Integer, FieldViewInjecting> viewIdMap = new LinkedHashMap<>();
    private InjectClass parentInjecting;
    private final String classPackage;
    private final String className;
    private final String targetClass;
    private final String classFqcn;


    /**
     * 绑定处理类
     *
     * @param classPackage 包名：com.butterknife
     * @param className    生成的类：MainActivity$$ViewBinder
     * @param targetClass  目标类：com.butterknife.MainActivity
     * @param classFqcn    生成Class的完全限定名称：com.butterknife.MainActivity$$ViewBinder
     */
    public InjectClass(String classPackage, String className, String targetClass, String classFqcn) {
        this.classPackage = classPackage;
        this.className = className;
        this.targetClass = targetClass;
        this.classFqcn = classFqcn;
    }

    /**
     * 生成Java类
     *
     * @return JavaFile
     */
    public JavaFile brewJava() {
        TypeSpec.Builder result = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(TypeVariableName.get("T", ClassName.bestGuess(targetClass)));

        if (_hasParentBinding()) {
            result.superclass(ParameterizedTypeName.get(ClassName.bestGuess(parentInjecting.classFqcn),
                    TypeVariableName.get("T")));
        } else {
            result.addSuperinterface(ParameterizedTypeName.get(VIEW_BINDER, TypeVariableName.get("T")));
        }

        result.addMethod(_createBindMethod());

        return JavaFile.builder(classPackage, result.build())
                .addFileComment("Generated code from Butter Knife. Do not modify!")
                .build();
    }

    /**
     * 创建方法
     *
     * @return MethodSpec
     */
    private MethodSpec _createBindMethod() {
        MethodSpec.Builder result = MethodSpec.methodBuilder("inject")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(FINDER, "finder", Modifier.FINAL)
                .addParameter(TypeVariableName.get("T"), "target", Modifier.FINAL)
                .addParameter(Object.class, "source");

        if (_hasParentBinding()) {
            // 调用父类的bind()方法
            result.addStatement("super.inject(finder, target, source)");
        }

        if (_hasViewBinding()) {
            // View
            for (Map.Entry<Integer, FieldViewInjecting> entry : viewIdMap.entrySet()) {
                int id = entry.getKey();
                FieldViewInjecting fieldViewInjecting = entry.getValue();
                result.addStatement("target.$L = finder.findRequiredView(source, $L, $S)",
                        fieldViewInjecting.getName(), id, fieldViewInjecting.getDescription());
            }

        }

        return result.build();
    }

    /**
     * 添加 ViewBinding
     *
     * @param injecting 资源信息
     */
    public void addViewBinding(int id, FieldViewInjecting injecting) {
        FieldViewInjecting fieldViewInjecting = viewIdMap.get(id);
        if (fieldViewInjecting == null) {
            viewBindings.add(injecting);
            viewIdMap.put(id, injecting);
        }
    }

    private boolean _hasViewBinding() {
        return !(viewBindings.isEmpty());
    }

    /**
     * 判断 id 是否已经绑定 View
     * @param id 资源ID
     * @return
     */
    public FieldViewInjecting isExistViewBinding(int id) {
        return viewIdMap.get(id);
    }


    /**
     * 设置父类
     *
     * @param parentInjecting InjectClass
     */
    public void setParentBinding(InjectClass parentInjecting) {
        this.parentInjecting = parentInjecting;
    }

    private boolean _hasParentBinding() {
        return parentInjecting != null;
    }
}
