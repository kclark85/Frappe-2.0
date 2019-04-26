
        import java.util.*;
        import java.io.*;
public class Lexer {
    public static String margin = "";
    // holds any number of tokens that have been put back
    private Stack<Token> stack;
    // the source of physical symbols
    // (use BufferedReader instead of Scanner because it can
    //  read a single physical symbol)
    private BufferedReader input;
    // one lookahead physical symbol
    private int lookahead;
    // construct a Lexer ready to produce tokens from a file
    public Lexer( String fileName ) {
        try {
            input = new BufferedReader( new FileReader( fileName ) );
        }
        catch(Exception e) {
            error("Problem opening file named [" + fileName + "]" );
        }
        stack = new Stack<Token>();
        lookahead = 0;  // indicates no lookahead symbol present
    }// constructor
    // produce the next token
    private Token getNext() {
        if( ! stack.empty() ) {
            //  produce the most recently putback token
            Token token = stack.pop();
            return token;
        }
        else {
            // produce a token from the input source
            int state = 1;  // state of FA
            String escapeCharData = ""; //used to add an escape character to a string
            String data = "";  // specific info for the token
            boolean done = false;
            int sym;  // holds current symbol
            do {
                sym = getNextSymbol();
                // System.out.println("current symbol: " + sym + " state = " + state );
                if ( state == 1 ) {
                    if ( sym == 9 || sym == 10 || sym == 13 ||
                            sym == 32 ) {// whitespace
                        state = 1;
                    }
                    else if (lowercase(sym) ) { //any lowercase
                        data += (char) sym;
                        state = 2; //go to name or token
                    }
                    else if ( uppercase(sym) ) {// any uppercase
                        data += (char) sym;
                        state = 3; //go to Classname
                    }
                    else if ( digit( sym ) ) {
                        data += (char) sym;
                        state = 11; //go to num (changed from 9 to 11)
                    }
                    else if ( sym == '-' ) {
                        data += (char) sym;
                        state = 10; //go to num (changed from 8 to 10)
                    }
                    else if ( sym == '"' ) {
                        state = 4; //go to string
                    }
                    else if ( sym == '{' || sym == '}' || sym == ';' ||
                            sym == '(' || sym == ')' ||
                            sym == ',' || sym == '=' || sym == '.'
                    ) {
                        data += (char) sym;
                        state = 13; // (changed from 11 to 13)
                        done = true;
                    }
                    else if ( sym == '/' ) {
                        state = 14; // (changed from 12 to 14)
                    }
                    else if ( sym == -1 ) {// end of file
                        state = 16; // (changed from 14 to 16)
                        done = true;
                    }
                    else {
                        error("Error in lexical analysis phase with symbol "
                                + sym + " in state " + state );
                    }
                }
                else if ( state == 2 ) {
                    if ( letter(sym) || digit(sym)) {
                        data += (char) sym;
                        state = 2;
                    }
                    else {// done with variable token
                        putBackSymbol( sym );
                        done = true;
                    }
                }
                else if ( state == 3 ) {
                    if ( letter(sym) || digit(sym)) {
                        data += (char) sym;
                        state = 3;
                    }
                    else {// done with Class token
                        putBackSymbol( sym );
                        done = true;
                    }
                }
                else if ( state == 4 ) { // String Token
                    if ( letter(sym) || digit(sym)) {
                        data += (char) sym;
                        state = 4;
                    }
                    else if(sym =='/'){
                        state = 5;
                    }
                    else if(sym =='"'){
                        state = 8; // (changed from 6 to 8)
                    }
                }
                else if ( state == 5 ) {// check for special char/instuction
                    if (digit(sym)) {
                        escapeCharData = "";
                        escapeCharData += (char) sym;
                        state = 6; // (changed 51 to 6)
                    }
                    else {
                        error("Error in lexical analysis phase with symbol "
                                + sym + " in state " + state );
                    }
                }
                else if ( state == 6 ) {// check for special char/instuction (changed 51 to 6)
                    if (digit(sym)){
                        escapeCharData += (char) sym;
                        state = 7; // (changed 52 to 7)
                    }
                    else {
                        error("Error in lexical analysis phase with symbol "
                                + sym + " in state " + state );
                    }
                }
                else if ( state == 7) {// check for special char/instuction (changed 52 to 7)
                    if (digit(sym)) {
                        escapeCharData += (char) sym;
                        data += (char) Integer.parseInt(escapeCharData);
                        state = 4;
                    }
                    else {
                        error("Error in lexical analysis phase with symbol "
                                + sym + " in state " + state );
                    }
                }
                else if ( state == 8 ) { // (changed 6 to 8
                    putBackSymbol( sym );
                    done = true;
                    return new Token( "str", data );

                }

                // note: states 9, and 10 are accepting states with
                //       no arcs out of them, so they are handled
                //       in the arc going into them
                else if ( state == 10 ) {// saw - neg. num (changed 8 to 10
                    if ( digit( sym ) ) {
                        data += (char) sym;
                        state = 11; //go to num (changed 9 to 11)
                    }
                    else {// saw something other than digit after -
                        error("Error in lexical analysis phase with symbol "
                                + sym + " in state " + state );
                    }
                }
                else if ( state == 11 ) {// saw num (changed 9 to 11)
                    if ( digit( sym ) ) {
                        data += (char) sym;
                        state = 11; //go to num (changed 9 to 11)
                    }
                    else if(sym == '.'){
                        data += (char) sym;
                        state = 12; //go to num (changed 10 to 12)
                    }
                    else {// saw something other than digit after -
                        putBackSymbol( sym );  // for next token
                        return new Token( "num",data );
                    }
                }
                else if ( state == 12 ) {// saw /, might be single or comment (changed 10 to 12)
                    if ( digit( sym ) ) {
                        data += (char) sym;
                        state = 12; //go to num (changed 10 to 12)
                    }
                    else {// saw something other than * after /
                        putBackSymbol( sym );  // for next token
                        return new Token( "num",data );
                    }
                }
                else if ( state == 14 ) {// looking for / to follow *? (changed 12 to 14)
                    if ( sym == '/' ) {// comment is done
                        state = 15;  // continue in this call to getNextToken (changed 13 to 15)
                        data = "";
                    }
                    else // saw something other than digit after -
                        error("Error in lexical analysis phase with symbol "
                                + sym + " in state " + state );
                }
                else if ( state == 15 ) {// looking for / to follow *? (changed 13 to 15)
                    if ( sym != 92 ) { // comment is done
                        state = 15;  // continue in this call to getNextToken (changed 13 to 15)
                        data = "";
                    }
                    else if ( sym == 92 ) { // comment is done
                        state = 1;  // continue in this call to getNextToken
                        data = "";
                    }
                    else // saw something other than digit after -
                        error("Error in lexical analysis phase with symbol "
                                + sym + " in state " + state );
                }
            }while( !done );
            // generate token depending on stopping state
            Token token;
            if ( state == 2 ) {
                // classes
                if ( data.equals("class") || data.equals("static") ||
                        data.equals("for") || data.equals("return") ||
                        data.equals("if") || data.equals("else") ||
                        data.equals("new") || data.equals("void") ||
                        data.equals("null") || data.equals("this") ||
                        data.equals("true") || data.equals("false")
                ) {
                    return new Token( data, "" );
                }
                else {
                    return new Token( "name", data );
                }
            }
            else if ( state == 13 ) { // (changed 11 to 13)
                // symbols
                if ( data.equals("(") || data.equals(")") ||
                        data.equals("{") || data.equals("}") ||
                        data.equals(";") || data.equals(",") ||
                        data.equals(".") || data.equals("=")
                ) {
                    return new Token( "single", data );
                }
                else {
                    error("Error in lexical analysis phase with symbol "
                            + sym + " in state " + state );
                    return null;
                }
            }
            else if ( state == 3 ) {
                return new Token( "className", data );
            }
            else if ( state == 11 || state == 12 ) { // (changed 9 to 11, 10 to 12)
                return new Token( "num", data );
            }
            else if ( state == 8) { // (changed 6 to 8)
                return new Token( "str", data );
            }
            else if ( state == 16 ) {// (changed 14 to 16)
                return new Token( "eof", data );
            }
            else {// Lexer error
                error("somehow Lexer FA halted in bad state " + state );
                return null;
            }
        }// else generate token from input
    }// getNext
    public Token getNextToken() {
        Token token = getNext();
        System.out.println("                     got token: " + token );
        return token;
    }
    public void putBackToken( Token token )
    {
        System.out.println( margin + "put back token " + token.toString() );
        stack.push( token );
    }
    // next physical symbol is the lookahead symbol if there is one,
    // otherwise is next symbol from file
    private int getNextSymbol() {
        int result = -1;
        if( lookahead == 0 ) {// is no lookahead, use input
            try{  result = input.read();  }
            catch(Exception e){}
        }
        else {// use the lookahead and consume it
            result = lookahead;
            lookahead = 0;
        }
        return result;
    }
    private void putBackSymbol( int sym ) {
        if( lookahead == 0 ) {// sensible to put one back
            lookahead = sym;
        }
        else {
            System.out.println("Oops, already have a lookahead " + lookahead +
                    " when trying to put back symbol " + sym );
            System.exit(1);
        }
    }// putBackSymbol
    private boolean letter( int code ) {
        return 'a'<=code && code<='z' ||
                'A'<=code && code<='Z';
    }
    private boolean uppercase( int code ) {
        return 'A'<=code && code<='Z';
    }
    private boolean lowercase( int code ) {
        return 'a'<=code && code<='z';
    }
    private boolean digit( int code ) {
        return '0'<=code && code<='9';
    }
    private boolean printable( int code ) {
        return ' '<=code && code<='~';
    }
    private static void error( String message ) {
        System.out.println( message );
        System.exit(1);
    }
    public static void main(String[] args) throws Exception {
        System.out.print("Enter file name: ");
        Scanner keys = new Scanner( System.in );
        String name = keys.nextLine();
        Lexer lex = new Lexer( name );
        Token token;
        do{
            token = lex.getNext();
            System.out.println( token.toString() );
        }while( ! token.getKind().equals( "eof" )  );
    }
}

