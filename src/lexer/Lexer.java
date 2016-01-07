package lexer;

import static control.Control.ConLexer.dump;

import java.io.InputStream;

import lexer.Token.Kind;
import util.Todo;

public class Lexer {
	String fname; // the input file name to be compiled
	InputStream fstream; // input stream for the above file
	int lineNum; // current line number
	int backCh;

	public Lexer(String fname, InputStream fstream) {
		this.fname = fname;
		this.fstream = fstream;
		this.lineNum = 1;
		this.backCh = -2;
	}

	// When called, return the next token (refer to the code "Token.java")
	// from the input stream.
	// Return TOKEN_EOF when reaching the end of the input stream.
	private Token nextTokenInternal() throws Exception {
		int c;

		if (this.backCh != -2) {
			c = this.backCh;
			this.backCh = -2;
		} else {
			c = this.fstream.read();
		}

		// handle comment, space, tab, newline, eof
		while (true) {
			if (-1 == c)
				// The value for "lineNum" is now "null",
				// you should modify this to an appropriate
				// line number for the "EOF" token.
				return new Token(Kind.TOKEN_EOF, lineNum);

			// skip all kinds of "blanks"
			while (' ' == c || '\t' == c || '\n' == c || '\r' == c) {
				if ('\n' == c)
					lineNum++;
				c = this.fstream.read();
			}
			if (-1 == c)
				return new Token(Kind.TOKEN_EOF, lineNum);

			if (c != '/')
				break;

			c = this.fstream.read();

			if (c == '/') {
				// //...
				while (c != -1 && c != '\n') {
					c = this.fstream.read();
				}
			} else if (c == '*') {
				// /*...*/
				new Todo();
			} else {
				throw new Exception();
			}
		}

		/*
		 * special token + - * = < && . , ! [] () {} ;
		 */
		Token token = null;
		System.out.println("start ch : " + String.valueOf((char) c) + " : " + c);
		switch (c) {
		case '+':
			token = new Token(Kind.TOKEN_ADD, lineNum);
			break;
		case '-':
			token = new Token(Kind.TOKEN_SUB, lineNum);
			break;
		case '*':
			token = new Token(Kind.TOKEN_TIMES, lineNum);
			break;
		case '=':
			token = new Token(Kind.TOKEN_ASSIGN, lineNum);
			break;
		case '<':
			token = new Token(Kind.TOKEN_LT, lineNum);
			break;
		case '&':
			c = this.fstream.read();
			if (c != '&') {
				throw new Exception();
			}
			token = new Token(Kind.TOKEN_AND, lineNum);
			break;
		case '.':
			token = new Token(Kind.TOKEN_DOT, lineNum);
			break;
		case ',':
			token = new Token(Kind.TOKEN_COMMER, lineNum);
			break;
		case '!':
			token = new Token(Kind.TOKEN_NOT, lineNum);
			break;
		case '{':
			token = new Token(Kind.TOKEN_LBRACE, lineNum);
			break;
		case '[':
			token = new Token(Kind.TOKEN_LBRACK, lineNum);
			break;
		case '(':
			token = new Token(Kind.TOKEN_LPAREN, lineNum);
			break;
		case '}':
			token = new Token(Kind.TOKEN_RBRACE, lineNum);
			break;
		case ']':
			token = new Token(Kind.TOKEN_RBRACK, lineNum);
			break;
		case ')':
			token = new Token(Kind.TOKEN_RPAREN, lineNum);
			break;
		case ';':
			token = new Token(Kind.TOKEN_SEMI, lineNum);
			break;
		}

		if (token == null) {
			String lexeme = "";

			if (c >= '0' && c <= '9') {
				while (c >= '0' && c <= '9') {
					lexeme += String.valueOf((char) c);
					c = this.fstream.read();
				}
				if (lexeme.startsWith("0") && !lexeme.equals("0")) {
					System.out.println("lexeme : " + lexeme);
					throw new Exception();
				}
				token = new Token(Kind.TOKEN_NUM, lineNum, lexeme);
				this.backCh = c;
			} else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
				while ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {
					lexeme += String.valueOf((char) c);
					c = this.fstream.read();
				}
				this.backCh = c;
				switch (lexeme) {
				case "boolean":
					token = new Token(Kind.TOKEN_BOOLEAN, lineNum);
					break;
				case "class":
					token = new Token(Kind.TOKEN_CLASS, lineNum);
					break;
				case "else":
					token = new Token(Kind.TOKEN_ELSE, lineNum);
					break;
				case "extends":
					token = new Token(Kind.TOKEN_EXTENDS, lineNum);
					break;
				case "false":
					token = new Token(Kind.TOKEN_FALSE, lineNum);
					break;
				case "if":
					token = new Token(Kind.TOKEN_IF, lineNum);
					break;
				case "int":
					token = new Token(Kind.TOKEN_INT, lineNum);
					break;
				case "length":
					token = new Token(Kind.TOKEN_LENGTH, lineNum);
					break;
				case "main":
					token = new Token(Kind.TOKEN_MAIN, lineNum);
					break;
				case "new":
					token = new Token(Kind.TOKEN_NEW, lineNum);
					break;
				case "out":
					token = new Token(Kind.TOKEN_OUT, lineNum);
					break;
				case "println":
					token = new Token(Kind.TOKEN_PRINTLN, lineNum);
					break;
				case "public":
					token = new Token(Kind.TOKEN_PUBLIC, lineNum);
					break;
				case "return":
					token = new Token(Kind.TOKEN_RETURN, lineNum);
					break;
				case "static":
					token = new Token(Kind.TOKEN_STATIC, lineNum);
					break;
				case "String":
					token = new Token(Kind.TOKEN_STRING, lineNum);
					break;
				case "System":
					token = new Token(Kind.TOKEN_SYSTEM, lineNum);
					break;
				case "this":
					token = new Token(Kind.TOKEN_THIS, lineNum);
					break;
				case "true":
					token = new Token(Kind.TOKEN_TRUE, lineNum);
					break;
				case "void":
					token = new Token(Kind.TOKEN_VOID, lineNum);
					break;
				case "while":
					token = new Token(Kind.TOKEN_WHILE, lineNum);
					break;
				default:
					token = new Token(Kind.TOKEN_ID, lineNum, lexeme);
				}
			}
		}

		// unknow symbol
		if (token == null)
			throw new Exception();

		return token;
	}

	public Token nextToken() {
		Token t = null;

		try {
			t = this.nextTokenInternal();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		if (dump)
			System.out.println(t.toString());
		return t;
	}
}
