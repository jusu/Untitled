package com.pinktwins.elephant.util;

public class StringParser {
    private Token pushback = null;

    private Token getNextToken(IParserInput aBuffer) {
        if (pushback != null) {
            Token lToken = pushback;
            pushback = null;
            return lToken;
        }

        if (aBuffer.eof())
            return new Token(Token.TokenType.eof, "Unexpected end of expression encountered.", aBuffer.getLineNr(), aBuffer.getColNr());
        else {
            // Keep track of the start of the token.
            final int lLine = aBuffer.getLineNr();
            final int lCol = aBuffer.getColNr();
            // We switch on the value of lChar to see what we have to do next.
            char lChar = aBuffer.consumeChar();
            switch (lChar) {
                case '"': {
                    // String literal encountered.
                    // Note that the starting " is skipped, it is not added to the value.
                    final StringBuilder lValue = new StringBuilder();
                    char lPeek = aBuffer.peekChar();
                    while (!aBuffer.eof() && ('"' != lPeek)) {
                        lValue.append(aBuffer.consumeChar());
                        lPeek = aBuffer.peekChar();
                    }

                    // We examine the two finishing conditions.
                    // The string is complete OR the buffer ended unexpectedly.
                    if (aBuffer.eof()) {
                        // EOF encountered, open string ...
                        return new Token(Token.TokenType.eof, String.format("Unclosed string starting on line: %d col: %d with contents \"%s\".", lLine, lCol, lValue.toString()), lLine, lCol);
                    } else {
                        // The string ended in a normal way.
                        // We skip the " delimiter without doing anything with it.
                        aBuffer.consumeChar();
                        return new Token(Token.TokenType.string, lValue.toString(), lLine, lCol);
                    }
                }
                default: {
                    if (Character.isWhitespace(lChar)) {
                        // Whitespace encountered. In this mode we gobble all whitespace and put it in
                        // a single token.
                        final StringBuilder lValue = new StringBuilder();
                        lValue.append(lChar);
                        while (!aBuffer.eof() && Character.isWhitespace(aBuffer.peekChar()))
                            lValue.append(aBuffer.consumeChar());
                        // We return the accumulated whitespace.
                        return new Token(Token.TokenType.whitespace, lValue.toString(), lLine, lCol);
                    } else {
                        // Normal identifier encountered. We turn this into a string as well.
                        // We have to test on set of characters that do not appear in any of the
                        // previous branches.
                        final StringBuilder lValue = new StringBuilder();
                        lValue.append(lChar);
                        char lPeek = aBuffer.peekChar();
                        while (!aBuffer.eof() && '(' != lPeek && ')' != lPeek && '"' != lPeek && !Character.isWhitespace(lPeek) && '=' != lPeek && ';' != lPeek) {
                            lValue.append(aBuffer.consumeChar());
                            lPeek = aBuffer.peekChar();
                        }
                        return new Token(Token.TokenType.string, lValue.toString(), lLine, lCol);
                    }
                }
            }
        }
    }

    private void pushBackToken(Token lToken) {
        pushback = lToken;
    }

    public Token getNextNonWhitespaceToken(IParserInput aBuffer) {
        Token lToken = getNextToken(aBuffer);
        while (lToken.isWhitespace()) lToken = getNextToken(aBuffer);
        return lToken;
    }

    public static class Token {
        // Difference between eof versus error.
        // We need this for the REPL. If a command is not complete, the user can continue
        // the command on a fresh line. If there is an error, the message will be printed and a new
        // command will be started.
        public enum TokenType {
            string, whitespace, error, eof
        };

        private String value;
        private TokenType type;

        private int line;
        private int col;

        public Token(TokenType aType, String aValue, int aLine, int aCol) {
            type = aType;
            value = aValue;
            line = aLine;
            col = aCol;
        }

        public TokenType getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public boolean isWhitespace() {
            return type == TokenType.whitespace;
        }

        public boolean isErroneous() {
            return isError() || isEof();
        }

        public boolean isError() {
            return type == TokenType.error;
        }

        public boolean isEof() {
            return type == TokenType.eof;
        }

        public boolean isString() {
            return type == TokenType.string;
        }

        public int getCol() {
            return col;
        }

        public int getLine() {
            return line;
        }
    }

    public interface IParserInput {
        char consumeChar();

        char peekChar();

        boolean eof();

        int getColNr();

        int getLineNr();
    }

    public static class Buffer
            implements IParserInput {
        // Essential data.
        private String sentence;
        private int pos;
        // Informative bookkeeping.
        // Can be useful for error messages.
        private int line;
        private int col;

        public Buffer(String aSentence) {
            sentence = aSentence;
            pos = 0;

            line = 1;
            col = 1;
        }

        public char consumeChar() {
            if (eof()) return 0;
            else {
                // Get the character.
                final char lChar = sentence.charAt(pos++);
                // Keep track of line/column count.
                if (lChar == '\n') {
                    line++;
                    col = 1;
                } else col++;
                return lChar;
            }
        }

        public char peekChar() {
            if (eof()) return 0;
            else return sentence.charAt(pos);
        }

        public boolean eof() {
            return (pos >= sentence.length());
        }

        public int getColNr() {
            return col;
        }

        public int getLineNr() {
            return line;
        }
    }
}
