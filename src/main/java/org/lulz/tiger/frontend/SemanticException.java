package org.lulz.tiger.frontend;

import org.antlr.v4.runtime.tree.ParseTree;

public class SemanticException extends RuntimeException {
    private ParseTree token;

    public SemanticException(ParseTree token, String message) {
        super(message);
        this.token = token;
    }

    public ParseTree getToken() {
        return token;
    }
}
