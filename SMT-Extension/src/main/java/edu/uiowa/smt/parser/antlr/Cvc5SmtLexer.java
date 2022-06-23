// Generated from Cvc5Smt.g4 by ANTLR 4.7.2
package edu.uiowa.smt.parser.antlr;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class Cvc5SmtLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		True=10, False=11, Quantifier=12, UnaryOperator=13, BinaryOperator=14, 
		TernaryOperator=15, MultiArityOperator=16, UninterpretedValue=17, Identifier=18, 
		IdentifierLetter=19, Integer=20, Digit=21, Comment=22, Whitespace=23;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
			"True", "False", "Quantifier", "UnaryOperator", "BinaryOperator", "TernaryOperator", 
			"MultiArityOperator", "UninterpretedValue", "Identifier", "IdentifierLetter", 
			"Integer", "Digit", "Comment", "Whitespace"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "')'", "'declare-sort'", "'define-fun'", "'Set'", "'Tuple'", 
			"'-'", "'as'", "'set.empty'", "'true'", "'false'", null, null, null, 
			"'ite'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, "True", "False", 
			"Quantifier", "UnaryOperator", "BinaryOperator", "TernaryOperator", "MultiArityOperator", 
			"UninterpretedValue", "Identifier", "IdentifierLetter", "Integer", "Digit", 
			"Comment", "Whitespace"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public Cvc5SmtLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Cvc5Smt.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\31\u015a\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\3\2"+
		"\3\2\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\5\3"+
		"\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7"+
		"\3\7\3\7\3\b\3\b\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3"+
		"\13\3\13\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3"+
		"\r\3\r\3\r\3\r\3\r\3\r\3\r\5\r~\n\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u00b7\n\16\3\17\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\5\17\u0107\n\17\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\5\21\u0129\n\21\3\22"+
		"\3\22\3\22\3\22\3\22\3\23\3\23\3\23\7\23\u0133\n\23\f\23\16\23\u0136\13"+
		"\23\3\23\3\23\7\23\u013a\n\23\f\23\16\23\u013d\13\23\3\23\5\23\u0140\n"+
		"\23\3\24\3\24\3\25\6\25\u0145\n\25\r\25\16\25\u0146\3\26\3\26\3\27\3\27"+
		"\7\27\u014d\n\27\f\27\16\27\u0150\13\27\3\27\3\27\3\30\6\30\u0155\n\30"+
		"\r\30\16\30\u0156\3\30\3\30\3\u013b\2\31\3\3\5\4\7\5\t\6\13\7\r\b\17\t"+
		"\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27"+
		"-\30/\31\3\2\6\5\2,-//\61\61\t\2$$&&))/\61C\\aac|\4\2\f\f\17\17\5\2\13"+
		"\f\17\17\"\"\2\u0176\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2"+
		"\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3"+
		"\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2"+
		"\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2"+
		"\2-\3\2\2\2\2/\3\2\2\2\3\61\3\2\2\2\5\63\3\2\2\2\7\65\3\2\2\2\tB\3\2\2"+
		"\2\13M\3\2\2\2\rQ\3\2\2\2\17W\3\2\2\2\21Y\3\2\2\2\23\\\3\2\2\2\25f\3\2"+
		"\2\2\27k\3\2\2\2\31}\3\2\2\2\33\u00b6\3\2\2\2\35\u0106\3\2\2\2\37\u0108"+
		"\3\2\2\2!\u0128\3\2\2\2#\u012a\3\2\2\2%\u013f\3\2\2\2\'\u0141\3\2\2\2"+
		")\u0144\3\2\2\2+\u0148\3\2\2\2-\u014a\3\2\2\2/\u0154\3\2\2\2\61\62\7*"+
		"\2\2\62\4\3\2\2\2\63\64\7+\2\2\64\6\3\2\2\2\65\66\7f\2\2\66\67\7g\2\2"+
		"\678\7e\2\289\7n\2\29:\7c\2\2:;\7t\2\2;<\7g\2\2<=\7/\2\2=>\7u\2\2>?\7"+
		"q\2\2?@\7t\2\2@A\7v\2\2A\b\3\2\2\2BC\7f\2\2CD\7g\2\2DE\7h\2\2EF\7k\2\2"+
		"FG\7p\2\2GH\7g\2\2HI\7/\2\2IJ\7h\2\2JK\7w\2\2KL\7p\2\2L\n\3\2\2\2MN\7"+
		"U\2\2NO\7g\2\2OP\7v\2\2P\f\3\2\2\2QR\7V\2\2RS\7w\2\2ST\7r\2\2TU\7n\2\2"+
		"UV\7g\2\2V\16\3\2\2\2WX\7/\2\2X\20\3\2\2\2YZ\7c\2\2Z[\7u\2\2[\22\3\2\2"+
		"\2\\]\7u\2\2]^\7g\2\2^_\7v\2\2_`\7\60\2\2`a\7g\2\2ab\7o\2\2bc\7r\2\2c"+
		"d\7v\2\2de\7{\2\2e\24\3\2\2\2fg\7v\2\2gh\7t\2\2hi\7w\2\2ij\7g\2\2j\26"+
		"\3\2\2\2kl\7h\2\2lm\7c\2\2mn\7n\2\2no\7u\2\2op\7g\2\2p\30\3\2\2\2qr\7"+
		"h\2\2rs\7q\2\2st\7t\2\2tu\7c\2\2uv\7n\2\2v~\7n\2\2wx\7g\2\2xy\7z\2\2y"+
		"z\7k\2\2z{\7u\2\2{|\7v\2\2|~\7u\2\2}q\3\2\2\2}w\3\2\2\2~\32\3\2\2\2\177"+
		"\u0080\7p\2\2\u0080\u0081\7q\2\2\u0081\u00b7\7v\2\2\u0082\u0083\7u\2\2"+
		"\u0083\u0084\7g\2\2\u0084\u0085\7v\2\2\u0085\u0086\7\60\2\2\u0086\u0087"+
		"\7u\2\2\u0087\u0088\7k\2\2\u0088\u0089\7p\2\2\u0089\u008a\7i\2\2\u008a"+
		"\u008b\7n\2\2\u008b\u008c\7g\2\2\u008c\u008d\7v\2\2\u008d\u008e\7q\2\2"+
		"\u008e\u00b7\7p\2\2\u008f\u0090\7u\2\2\u0090\u0091\7g\2\2\u0091\u0092"+
		"\7v\2\2\u0092\u0093\7\60\2\2\u0093\u0094\7e\2\2\u0094\u0095\7q\2\2\u0095"+
		"\u0096\7o\2\2\u0096\u0097\7r\2\2\u0097\u0098\7n\2\2\u0098\u0099\7g\2\2"+
		"\u0099\u009a\7o\2\2\u009a\u009b\7g\2\2\u009b\u009c\7p\2\2\u009c\u00b7"+
		"\7v\2\2\u009d\u009e\7t\2\2\u009e\u009f\7g\2\2\u009f\u00a0\7n\2\2\u00a0"+
		"\u00a1\7\60\2\2\u00a1\u00a2\7v\2\2\u00a2\u00a3\7t\2\2\u00a3\u00a4\7c\2"+
		"\2\u00a4\u00a5\7p\2\2\u00a5\u00a6\7u\2\2\u00a6\u00a7\7r\2\2\u00a7\u00a8"+
		"\7q\2\2\u00a8\u00a9\7u\2\2\u00a9\u00b7\7g\2\2\u00aa\u00ab\7t\2\2\u00ab"+
		"\u00ac\7g\2\2\u00ac\u00ad\7n\2\2\u00ad\u00ae\7\60\2\2\u00ae\u00af\7v\2"+
		"\2\u00af\u00b0\7e\2\2\u00b0\u00b1\7n\2\2\u00b1\u00b2\7q\2\2\u00b2\u00b3"+
		"\7u\2\2\u00b3\u00b4\7w\2\2\u00b4\u00b5\7t\2\2\u00b5\u00b7\7g\2\2\u00b6"+
		"\177\3\2\2\2\u00b6\u0082\3\2\2\2\u00b6\u008f\3\2\2\2\u00b6\u009d\3\2\2"+
		"\2\u00b6\u00aa\3\2\2\2\u00b7\34\3\2\2\2\u00b8\u0107\4?@\2\u00b9\u00ba"+
		"\7@\2\2\u00ba\u0107\7?\2\2\u00bb\u0107\7>\2\2\u00bc\u00bd\7>\2\2\u00bd"+
		"\u0107\7?\2\2\u00be\u0107\t\2\2\2\u00bf\u00c0\7o\2\2\u00c0\u00c1\7q\2"+
		"\2\u00c1\u0107\7f\2\2\u00c2\u00c3\7?\2\2\u00c3\u0107\7@\2\2\u00c4\u00c5"+
		"\7u\2\2\u00c5\u00c6\7g\2\2\u00c6\u00c7\7v\2\2\u00c7\u00c8\7\60\2\2\u00c8"+
		"\u00c9\7w\2\2\u00c9\u00ca\7p\2\2\u00ca\u00cb\7k\2\2\u00cb\u00cc\7q\2\2"+
		"\u00cc\u0107\7p\2\2\u00cd\u00ce\7u\2\2\u00ce\u00cf\7g\2\2\u00cf\u00d0"+
		"\7v\2\2\u00d0\u00d1\7\60\2\2\u00d1\u00d2\7k\2\2\u00d2\u00d3\7p\2\2\u00d3"+
		"\u00d4\7v\2\2\u00d4\u00d5\7g\2\2\u00d5\u0107\7t\2\2\u00d6\u00d7\7u\2\2"+
		"\u00d7\u00d8\7g\2\2\u00d8\u00d9\7v\2\2\u00d9\u00da\7\60\2\2\u00da\u00db"+
		"\7o\2\2\u00db\u00dc\7k\2\2\u00dc\u00dd\7p\2\2\u00dd\u00de\7w\2\2\u00de"+
		"\u0107\7u\2\2\u00df\u00e0\7u\2\2\u00e0\u00e1\7g\2\2\u00e1\u00e2\7v\2\2"+
		"\u00e2\u00e3\7\60\2\2\u00e3\u00e4\7o\2\2\u00e4\u00e5\7g\2\2\u00e5\u00e6"+
		"\7o\2\2\u00e6\u00e7\7d\2\2\u00e7\u00e8\7g\2\2\u00e8\u0107\7t\2\2\u00e9"+
		"\u00ea\7u\2\2\u00ea\u00eb\7g\2\2\u00eb\u00ec\7v\2\2\u00ec\u00ed\7\60\2"+
		"\2\u00ed\u00ee\7u\2\2\u00ee\u00ef\7w\2\2\u00ef\u00f0\7d\2\2\u00f0\u00f1"+
		"\7u\2\2\u00f1\u00f2\7g\2\2\u00f2\u0107\7v\2\2\u00f3\u00f4\7t\2\2\u00f4"+
		"\u00f5\7g\2\2\u00f5\u00f6\7n\2\2\u00f6\u00f7\7\60\2\2\u00f7\u00f8\7l\2"+
		"\2\u00f8\u00f9\7q\2\2\u00f9\u00fa\7k\2\2\u00fa\u0107\7p\2\2\u00fb\u00fc"+
		"\7t\2\2\u00fc\u00fd\7g\2\2\u00fd\u00fe\7n\2\2\u00fe\u00ff\7\60\2\2\u00ff"+
		"\u0100\7r\2\2\u0100\u0101\7t\2\2\u0101\u0102\7q\2\2\u0102\u0103\7f\2\2"+
		"\u0103\u0104\7w\2\2\u0104\u0105\7e\2\2\u0105\u0107\7v\2\2\u0106\u00b8"+
		"\3\2\2\2\u0106\u00b9\3\2\2\2\u0106\u00bb\3\2\2\2\u0106\u00bc\3\2\2\2\u0106"+
		"\u00be\3\2\2\2\u0106\u00bf\3\2\2\2\u0106\u00c2\3\2\2\2\u0106\u00c4\3\2"+
		"\2\2\u0106\u00cd\3\2\2\2\u0106\u00d6\3\2\2\2\u0106\u00df\3\2\2\2\u0106"+
		"\u00e9\3\2\2\2\u0106\u00f3\3\2\2\2\u0106\u00fb\3\2\2\2\u0107\36\3\2\2"+
		"\2\u0108\u0109\7k\2\2\u0109\u010a\7v\2\2\u010a\u010b\7g\2\2\u010b \3\2"+
		"\2\2\u010c\u010d\7v\2\2\u010d\u010e\7w\2\2\u010e\u010f\7r\2\2\u010f\u0110"+
		"\7n\2\2\u0110\u0129\7g\2\2\u0111\u0112\7u\2\2\u0112\u0113\7g\2\2\u0113"+
		"\u0114\7v\2\2\u0114\u0115\7\60\2\2\u0115\u0116\7k\2\2\u0116\u0117\7p\2"+
		"\2\u0117\u0118\7u\2\2\u0118\u0119\7g\2\2\u0119\u011a\7t\2\2\u011a\u0129"+
		"\7v\2\2\u011b\u011c\7f\2\2\u011c\u011d\7k\2\2\u011d\u011e\7u\2\2\u011e"+
		"\u011f\7v\2\2\u011f\u0120\7k\2\2\u0120\u0121\7p\2\2\u0121\u0122\7e\2\2"+
		"\u0122\u0129\7v\2\2\u0123\u0124\7q\2\2\u0124\u0129\7t\2\2\u0125\u0126"+
		"\7c\2\2\u0126\u0127\7p\2\2\u0127\u0129\7f\2\2\u0128\u010c\3\2\2\2\u0128"+
		"\u0111\3\2\2\2\u0128\u011b\3\2\2\2\u0128\u0123\3\2\2\2\u0128\u0125\3\2"+
		"\2\2\u0129\"\3\2\2\2\u012a\u012b\7B\2\2\u012b\u012c\5%\23\2\u012c\u012d"+
		"\7a\2\2\u012d\u012e\5)\25\2\u012e$\3\2\2\2\u012f\u0134\5\'\24\2\u0130"+
		"\u0133\5\'\24\2\u0131\u0133\5+\26\2\u0132\u0130\3\2\2\2\u0132\u0131\3"+
		"\2\2\2\u0133\u0136\3\2\2\2\u0134\u0132\3\2\2\2\u0134\u0135\3\2\2\2\u0135"+
		"\u0140\3\2\2\2\u0136\u0134\3\2\2\2\u0137\u013b\7~\2\2\u0138\u013a\13\2"+
		"\2\2\u0139\u0138\3\2\2\2\u013a\u013d\3\2\2\2\u013b\u013c\3\2\2\2\u013b"+
		"\u0139\3\2\2\2\u013c\u013e\3\2\2\2\u013d\u013b\3\2\2\2\u013e\u0140\7~"+
		"\2\2\u013f\u012f\3\2\2\2\u013f\u0137\3\2\2\2\u0140&\3\2\2\2\u0141\u0142"+
		"\t\3\2\2\u0142(\3\2\2\2\u0143\u0145\5+\26\2\u0144\u0143\3\2\2\2\u0145"+
		"\u0146\3\2\2\2\u0146\u0144\3\2\2\2\u0146\u0147\3\2\2\2\u0147*\3\2\2\2"+
		"\u0148\u0149\4\62;\2\u0149,\3\2\2\2\u014a\u014e\7=\2\2\u014b\u014d\n\4"+
		"\2\2\u014c\u014b\3\2\2\2\u014d\u0150\3\2\2\2\u014e\u014c\3\2\2\2\u014e"+
		"\u014f\3\2\2\2\u014f\u0151\3\2\2\2\u0150\u014e\3\2\2\2\u0151\u0152\b\27"+
		"\2\2\u0152.\3\2\2\2\u0153\u0155\t\5\2\2\u0154\u0153\3\2\2\2\u0155\u0156"+
		"\3\2\2\2\u0156\u0154\3\2\2\2\u0156\u0157\3\2\2\2\u0157\u0158\3\2\2\2\u0158"+
		"\u0159\b\30\2\2\u0159\60\3\2\2\2\16\2}\u00b6\u0106\u0128\u0132\u0134\u013b"+
		"\u013f\u0146\u014e\u0156\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}