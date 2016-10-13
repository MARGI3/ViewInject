package com.margi.compile;

import com.google.auto.service.AutoService;
import com.margi.annotation.InjectView;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Created by allen on 8/24/16.
 *
 * 示例代码的解释说明

 init(ProcessingEnvironment env):
 每一个注解处理器类都必须有一个空的构造函数。然而，这里有一个特殊的init()方法，它会被注解处理工具调用，并输入ProcessingEnviroment参数。

 ProcessingEnviroment提供很多有用的工具类Elements, Types和Filer。后面我们将看到详细的内容。

 process(Set<? extends TypeElement> annotations, RoundEnvironment env): 这相当于每个处理器的主函数main()。
 你在这里写你的扫描、评估和处理注解的代码，以及生成Java文件。输入参数RoundEnviroment，可以让你查询出包含特定注解的被注解元素。后面我们将看到详细的内容。

 getSupportedAnnotationTypes(): 这里你必须指定，这个注解处理器是注册给哪个注解的。
 注意，它的返回值是一个字符串的集合，包含本处理器想要处理的注解类型的合法全称。换句话说，你在这里定义你的注解处理器注册到哪些注解上。

 getSupportedSourceVersion(): 用来指定你使用的Java版本。通常这里返回SourceVersion.latestSupported()。
 然而，如果你有足够的理由只支持Java 6的话，你也可以返回SourceVersion.RELEASE_6。推荐你使用前者。


 可以使用重载getSupportedAnnotationTypes()和getSupportedSourceVersion()方法代替@SupportedAnnotationTypes和@SupportedSourceVersion。
 *
 */
@AutoService(Processor.class)
public class InjectorProcessor  extends AbstractProcessor{


    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 保存包含注解元素的目标类，注意是使用注解的外围类，主要用来处理父类继承，例：MainActivity
        Set<TypeElement> erasedTargetNames = new LinkedHashSet<>();
        // TypeElement 使用注解的外围类， 对应一个要生成的类
        Map<TypeElement, InjectClass> targetClassMap = new LinkedHashMap<>();

        // 处理Bind
        for (Element element : roundEnv.getElementsAnnotatedWith(InjectView.class)) {
            if (VerifyHelper.verifyView(element, messager)) {
                ParseHelper.parseViewBind(element, targetClassMap, erasedTargetNames,
                        elementUtils, typeUtils, messager);
                _log(element, "element getSimpleName =  %s  getEnclosingElement = %s ",element.getSimpleName(),element.getEnclosingElement());
            }
        }

        for (Map.Entry<TypeElement, InjectClass> entry : targetClassMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            InjectClass injectClass = entry.getValue();

            _log(typeElement,"second step type element = %s ",typeElement);
            _log(typeElement,"second step type element simple name = %s ",typeElement.getSimpleName());

            // 查看是否父类也进行注解绑定，有则添加到BindingClass
            TypeElement parentType = _findParentType(typeElement, erasedTargetNames);
            if (parentType != null) {
                InjectClass parentBinding = targetClassMap.get(parentType);
                injectClass.setParentBinding(parentBinding);
            }

            try {
                // 生成Java文件
                injectClass.brewJava().writeTo(filer);
            } catch (IOException e) {
                _error(typeElement, "Unable to write view binder for type %s: %s", typeElement,
                        e.getMessage());
            }
        }

        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(InjectView.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }



    /**
     * 查找父类型
     * @param typeElement   类元素
     * @param erasedTargetNames 存在的类元素
     * @return
     */
    private TypeElement _findParentType(TypeElement typeElement, Set<TypeElement> erasedTargetNames) {
        TypeMirror typeMirror;
        while (true) {
            // 父类型要通过 TypeMirror 来获取
            typeMirror = typeElement.getSuperclass();
            if (typeMirror.getKind() == TypeKind.NONE) {
                return null;
            }
            // 获取父类元素
            typeElement = (TypeElement) ((DeclaredType)typeMirror).asElement();
            if (erasedTargetNames.contains(typeElement)) {
                // 如果父类元素存在则返回
                return typeElement;
            }
        }
    }


    /**
     * 输出错误信息
     * @param element
     * @param message
     * @param args
     */
    private void _error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }


    private void _log(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message, element);
    }

}
