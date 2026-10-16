package com.docguard.model;

import java.util.Objects;

public final class Parameter {
    private final String name;
    private final String type;
    private final String kind;

    public Parameter(String name, String type, String kind) {
        this.name = name;
        this.type = type;
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getKind() {
        return kind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameter parameter = (Parameter) o;
        return Objects.equals(name, parameter.name)
                && Objects.equals(type, parameter.type)
                && Objects.equals(kind, parameter.kind);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, kind);
    }
}
