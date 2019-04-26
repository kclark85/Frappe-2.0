

/*
    This class provides a recursive descent parser
    for Frappe,
    creating a parse tree which can be interpreted
    to simulate execution of a Frappe program
*/

        import java.util.*;
        import java.io.*;

public class Parser {

    private Lexer lex;

    public Parser( Lexer lexer ) {
        lex = lexer;
    }

    public Node parseProgram() {
        System.out.println("-----> parsing <program>:");
        Node first = parseClasses();
        return new Node("program", first, null, null);
    }

    public Node parseClasses() {
        System.out.println("-----> parsing <classes>:");
        Node first = parseClass();
        Token token = lex.getNextToken();
        if ( token.isKind("eof")) { //there are no more classes
            return new Node("classes", first, null, null);
        }
        else { //there is another class
            lex.putBackToken(token);
            Node second = parseClasses();
            return new Node("classes", first, second, null);
        }
    }

    public Node parseClass() {
        System.out.println("-----> parsing <class>:");
        Token token = lex.getNextToken();
        errorCheck(token, "class");
        Token name = lex.getNextToken();
        errorCheck(name, "className");
        token = lex.getNextToken();
        errorCheck(token, "single", "{");
        token = lex.getNextToken();
        if(!token.isKind("single")) {
            lex.putBackToken(token);
            Node first = parseMembers();
            token = lex.getNextToken();
            errorCheck(token, "single", "}");
            return new Node("class", name.getDetails(), first, null, null);
        }
        else {
            errorCheck(token, "single", "}");
            return new Node("class", name.getDetails(), null, null, null);
        }
    }

    public Node parseMembers() {
        System.out.println("-----> parsing <members>:");
        Node first = parseMember();
        Token token = lex.getNextToken();
        if (token.matches("single", "}")) { //there are no more members
            lex.putBackToken(token);
            return new Node("members", first, null, null);
        }
        else { //there is another member
            lex.putBackToken(token);
            Node second = parseMembers();
            return new Node("members", first, second, null);
        }
    }

    public Node parseMember() {
        System.out.println("-----> parsing <member>:");
        Token token = lex.getNextToken();
        if (token.isKind("static")) { //child might be a <staticMethod> or <staticField>
            // Token name = lex.getNextToken();
            Token second = lex.getNextToken();
            errorCheck(second, "name");
            Token third = lex.getNextToken();
            if (third.matches("single", "(")) { //child is a <staticMethod>
                lex.putBackToken(third);
                lex.putBackToken(second);
                lex.putBackToken(token);
                Node first = parseStaticMethod();
                return new Node("member", first, null, null);
            }
            else { //child is a <staticField>
                lex.putBackToken(third);
                lex.putBackToken(second);
                lex.putBackToken(token);
                Node first = parseStaticField();
                return new Node("member", first, null, null);
            }
        }
        else if (token.isKind("name")) { //child might be a <instanceField> or <instanceMethod>
            Token name = token;
            token = lex.getNextToken();
            if (token.matches("single", "(")) { //child is an <instanceMethod>
                lex.putBackToken(token);
                lex.putBackToken(name);
                Node first = parseInstanceMethod();
                return new Node("member", first, null, null);
            }
            else { //child is an <instanceField>
                lex.putBackToken(token);
                lex.putBackToken(name);
                Node first = parseInstanceField();
                return new Node("member", first, null, null);
            }
        }
        else if (token.isKind("className")) { //child is a <constructor>
            lex.putBackToken(token);
            Node first = parseConstructor();
            return new Node("member", first, null, null);
        }
        else { // error
            System.out.println("expected static or name or className and saw " + token );
            System.exit(1);
            return null;
        }
    }

    public Node parseStaticField() {
        System.out.println("-----> parsing <staticField>:");
        Token token = lex.getNextToken();
        errorCheck(token, "static");
        Token name = lex.getNextToken();
        errorCheck(name, "name");
        token = lex.getNextToken();
        if (token.matches("single", "=")) { //child is an <expression>
            Node first = parseExpression();
            return new Node("staticField", name.getDetails(), first, null, null);
        }
        else { //node is a declaration
            lex.putBackToken(token);
            return new Node("staticField", name.getDetails(), null, null, null);
        }
    }

    public Node parseStaticMethod() {
        System.out.println("-----> parsing <staticMethod>:");
        Token token = lex.getNextToken();
        errorCheck(token, "static");
        Token name = lex.getNextToken();
        errorCheck(name, "name");
        Node first = parseRestOfMethod();
        return new Node("staticMethod", name.getDetails(), first, null, null);
    }

    public Node parseInstanceField() {
        System.out.println("-----> parsing <instanceField>:");
        Token name = lex.getNextToken();
        errorCheck(name, "name");
        Token token = lex.getNextToken();
        if (token.matches("single", "=")) { //child is an <expression>
            Node first = parseExpression();
            return new Node("instanceField", name.getDetails(), first, null, null);
        }
        else { //node is a declaration
            lex.putBackToken(token);
            return new Node("instanceField", name.getDetails(), null, null, null);
        }

    }

    public Node parseConstructor() {
        System.out.println("-----> parsing <constructor>:");
        Token name = lex.getNextToken();
        errorCheck(name, "className");
        Node first = parseRestOfMethod();
        return new Node("constructor", name.getDetails(), first, null, null);
    }

    public Node parseInstanceMethod() {
        System.out.println("-----> parsing <instanceMethod>:");
        Token name = lex.getNextToken();
        errorCheck(name, "name");
        Node first = parseRestOfMethod();
        return new Node("instanceMethod", name.getDetails(), first, null, null);
    }

    public Node parseRestOfMethod() {
        System.out.println("-----> parsing <restOfMethod>");
        Token token = lex.getNextToken();
        errorCheck(token, "single", "(");
        token = lex.getNextToken();
        if(token.matches("single",")")) { // no params
            //token = lex.getNextToken();
            Node first = parseMethodBody();
            return new Node("restOfMethod", first, null, null);
        }
        else { // have params
            Node first = parseParams();
            token = lex.getNextToken();
            errorCheck(token, "single", ")");
            //token = lex.getNextToken();
            Node second = parseMethodBody();
            return new Node("restOfMethod", first, second, null);
        }
    }

    public Node parseParams() {
        System.out.println("-----> parsing <params>:");
        Token name = lex.getNextToken();
        errorCheck(name, "name");
        Token token = lex.getNextToken();
        if(token.matches("single", ",")){ // have more params
            token = lex.getNextToken();
            Node first = parseParams();
            return new Node("params", name.getDetails(), first, null, null);
        }
        else { // no more params
            lex.putBackToken(token);
            return new Node("params", name.getDetails(), null, null, null);
        }
    }

    public Node parseMethodBody() {
        System.out.println("-----> parsing <methodBody>");
        Token token = lex.getNextToken();
        errorCheck(token, "single", "{");
        token = lex.getNextToken();
        if(token.matches("single", "}")){ // no statements
            return new Node("methodBody", null, null, null);
        }
        else { // have statements
            lex.putBackToken(token);
            Node first = parseStatements();
            token = lex.getNextToken();
            errorCheck(token, "single", "}");
            return new Node("methodBody", first, null, null);
        }
    }

    public Node parseStatements() {
        System.out.println("-----> parsing <statements>:");
        Node first = parseStatement();
        Token token = lex.getNextToken();
        if(token.matches("single", "}")){ // no more statements
            lex.putBackToken(token);
            return new Node("statements", first, null, null);
        }
        else {
            lex.putBackToken(token);
            Node second = parseStatements();
            return new Node("statements", first, second, null);
        }
    }

    public Node parseStatement() {
        System.out.println("-----> parsing <statement>:");
        Token token = lex.getNextToken();
        if(token.isKind("name")) { // <rhs> ~OR~ <refChain>
            Token second = lex.getNextToken();
            if(second.matches("single", "=")) { // NAME EQUALS <rhs>
                Node first = parseRHS();
                return new Node("statement", first, null, null);
            }
            else { // <refChain>
                lex.putBackToken(token);
                lex.putBackToken(second);
                Node first = parseRefChain();
                return new Node("statement", first, null, null);
            }
        }
        else if(token.isKind("className")) { // <refChain>
            lex.putBackToken(token);
            Node first = parseRefChain();
            return new Node("statement", first, null, null);
        }
        else if(token.isKind("while")) { // <whileStatement>
            lex.putBackToken(token);
            Node first = parseWhileStatement();
            return new Node("statement", first, null, null);
        }
        else if(token.isKind("if")) { // <ifStatement>
            lex.putBackToken(token);
            Node first = parseIfStatement();
            return new Node("statement", first, null, null);
        }
        else {
            errorCheck(token, "return");
            Node first = parseExpression();
            return new Node("statement", first, null, null);
        }
    }

    public Node parseWhileStatement() {
        System.out.println("----> parsing <whileStatement>");
        Token token = lex.getNextToken();
        errorCheck(token, "while");
        token = lex.getNextToken();
        errorCheck(token, "single", "(");
        Node first = parseExpression();
        token = lex.getNextToken();
        errorCheck(token, "single", ")");
        Node second = parseLoopBody();
        return new Node("whileStatement", first, second, null);
    }

    public Node parseIfStatement() {
        System.out.println("----> parsing <ifStatement>");
        Token token = lex.getNextToken();
        errorCheck(token, "if");
        token = lex.getNextToken();
        errorCheck(token,"single", "(");
        Node first = parseExpression();
        token = lex.getNextToken();
        errorCheck(token,"single", ")");
        token = lex.getNextToken();
        errorCheck(token,"single", "{");
        token = lex.getNextToken();
        if (token.matches("single", "}")) {
            token = lex.getNextToken();
            if (token.isKind("else")) {
                token = lex.getNextToken();
                errorCheck(token, "single", "{");
                token = lex.getNextToken();
                if (token.matches("single", "}")) { //IF LPAREN <expression> RPAREN LBRACE RBRACE ELSE LBRACE RBRACE
                    return new Node("ifStatement", first, null, null);
                }
                else { //IF LPAREN <expression> RPAREN LBRACE RBRACE ELSE LBRACE <statements> RBRACE
                    lex.putBackToken(token);
                    Node third = parseStatements();
                    token = lex.getNextToken();
                    errorCheck(token, "single", "}");
                    return new Node("ifStatement", first, null, third);
                }
            }
            else { //IF LPAREN <expression> RPAREN LBRACE RBRACE
                lex.putBackToken(token);
                return new Node("ifStatement", first, null, null);
            }
        }
        else {
            lex.putBackToken(token);
            Node second = parseStatements();
            token = lex.getNextToken();
            errorCheck(token, "single", "}");
            token = lex.getNextToken();
            if (token.isKind("else")) {
                token = lex.getNextToken();
                errorCheck(token, "single", "{");
                token = lex.getNextToken();
                if (token.matches("single", "}")) { //IF LPAREN <expression> RPAREN LBRACE <statements> RBRACE ELSE LBRACE RBRACE
                    return new Node("ifStatement", first, second, null);
                }
                else { //IF LPAREN <expression> RPAREN LBRACE <statements> RBRACE ELSE LBRACE <statements> RBRACE
                    lex.putBackToken(token);
                    Node third = parseStatements();
                    token = lex.getNextToken();
                    errorCheck(token, "single", "}");
                    return new Node("ifStatement", first, second, third);
                }
            }
            else { //IF LPAREN <expression> RPAREN LBRACE <statements> RBRACE
                lex.putBackToken(token);
                return new Node("ifStatement", first, second, null);
            }
        }
    }


    public Node parseRHS() {
        System.out.println("-----> parsing <rhs>:");
        Token token = lex.getNextToken();
        if(!token.isKind("new")){  //if the RHS is an expression
            lex.putBackToken(token);
            Node first = parseExpression();
            return new Node("RHS", first, null, null);
        }
        else { // NEW CLASSNAME <argsPart>
            errorCheck(token, "new"); //if the RHS is a class declaration
            Token className = lex.getNextToken();
            errorCheck(className, "className");
            Node first = parseArgsPart();
            return new Node("RHS", first, null, null);
        }
        //Token single = lex.getNextToken();
        //errorCheck(single, "single", "(");
        //Token args = lex.getNextToken();
        //if(args.getDetails()==")"){
        //   return new Node("statement", null, null, null);
        //}
        //else{
        //   lex.putBackToken(args);
        //   Node first = parseArgs();
        //   return new Node("statement", first, null,null);
        //}
        //return null;
    }

    public Node parseLoopBody() {
        System.out.println("----> parsing <loopBody>");
        Token token = lex.getNextToken();
        errorCheck(token, "single", "{");
        token = lex.getNextToken();
        if(token.matches("single", "}")) { // no statements
            return new Node("loopBody", null, null, null);
        }
        else { // has statements
            lex.putBackToken(token);
            Node first = parseStatements();
            token = lex.getNextToken();
            errorCheck(token, "single", "}");
            return new Node("loopBody", first, null, null);
        }
    }

    public Node parseExpression() {
        //deal with ")" from while loop
        System.out.println("-----> parsing <expression>:");
        Token token = lex.getNextToken();
        //if(token.getDetails().equalsIgnoreCase("str") || token.getDetails().equalsIgnoreCase("num")||
        //        token.getDetails().equalsIgnoreCase("null") || token.getDetails().equalsIgnoreCase("this")||
        //        token.getDetails().equalsIgnoreCase("true") || token.getDetails().equalsIgnoreCase("false")){
        if(token.isKind("str") || token.isKind("num") ||
                token.isKind("null") || token.isKind("this") ||
                token.isKind("true") || token.isKind("false")) {
            return new Node(token.getKind(), token.getDetails(), null, null, null); //not sure if k would be "expression" or "statement"
        }
        else{
            lex.putBackToken(token);
            Node first = parseRefChain();
            return new Node("statement", first, null, null);
        }
    }

    public Node parseRefChain() {
        System.out.println("-----> parsing <refChain>:");
        Node first = parseCaller();
        Token token = lex.getNextToken();
        if(token.getDetails() == "."){
            Node second = parseRefChain();
            return new Node("statement", first, second, null);
        }
        else {
            lex.putBackToken(token);
            return new Node("statement", first, null, null);
        }
    }

    public Node parseCaller() {
        System.out.println("-----> parsing <caller>:");
        Token name = lex.getNextToken();
        if(name.isKind("className")) { // CLASSNAME
            return new Node("caller", name.getDetails(), null, null, null);
        }
        else { // NAME | NAME <argsPart>
            errorCheck(name, "name");
            Token token = lex.getNextToken();
            if(token.matches("single", "(")) { // NAME <argsPart>
                lex.putBackToken(token);
                Node first = parseArgsPart();
                return new Node("caller", name.getDetails(), first, null, null);
            }
            else { // NAME
                lex.putBackToken(token);
                return new Node("caller", name.getDetails(), null, null, null);
            }
        }
    }

    public Node parseArgsPart() {
        System.out.println("-----> parsing <argsPart>:");
        Token token = lex.getNextToken();
        errorCheck(token, "single", "(");
        token = lex.getNextToken();
        if(token.matches("single",")")) { // no args
            //Node first = parseArgsPart();
            //return new Node("argsPart", first, null, null);
            return new Node("argsPart", null, null, null);
        }
        else{
            //errorCheck(token, "name", "args");
            lex.putBackToken(token);
            Node first = parseArgs();
            token = lex.getNextToken();
            errorCheck(token, "single", ")");
            return new Node("argsPart", first, null, null);
        }
    }

    public Node parseArgs() {
        System.out.println("-----> parsing <args>:");
        //Token token = lex.getNextToken();
        //errorCheck(token, "name", "args");
        //lex.putBackToken(token);
        Node first = parseExpression();
        Token token = lex.getNextToken();
        if(token.matches("single", ")")) {
            lex.putBackToken(token);
            return new Node("args", first, null, null);
        }
        else if(token.matches("single", ",")) { // <expression>, <args>
            Node second = parseArgs();
            return new Node("args", first, second, null);
        }
        else{ // <expression>
            lex.putBackToken(token);
            return new Node("args", first, null, null);
        }
    }

    // check whether token is correct kind
    private void errorCheck( Token token, String kind ) {
        if( ! token.isKind( kind ) ) {
            System.out.println("Error:  expected " + token +
                    " to be of kind " + kind );
            System.exit(1);
        }
    }

    // check whether token is correct kind and details
    private void errorCheck( Token token, String kind, String details ) {
        if( ! token.isKind( kind ) ||
                ! token.getDetails().equals( details ) ) {
            System.out.println("Error:  expected " + token +
                    " to be kind= " + kind +
                    " and details= " + details );
            System.exit(1);
        }
    }

}

