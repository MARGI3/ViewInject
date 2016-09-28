package com.margi.compile;

import com.squareup.javapoet.TypeName;

import static com.margi.compile.ParseHelper.VIEW_TYPE;

/**
 * View绑定信息
 */
final class FieldViewInjecting implements ViewInjecting {
    private final String name;
    private final TypeName type;
    private final boolean required;

    FieldViewInjecting(String name, TypeName type, boolean required) {
        this.name = name;
        this.type = type;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public TypeName getType() {
        return type;
    }

    @Override
    public String getDescription() {
        return "field '" + name + "'";
    }

    public boolean isRequired() {
        return required;
    }

    public boolean requiresCast() {
        return !VIEW_TYPE.equals(type.toString());
    }
}
