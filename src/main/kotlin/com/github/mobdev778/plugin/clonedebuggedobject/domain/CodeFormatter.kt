package com.github.mobdev778.plugin.clonedebuggedobject.domain

object CodeFormatter {

    private const val INDENT = 4

    fun format(code: String): String {
        val tokens = tokenize(code)
        val builder = StringBuilder()
        var indent = 0
        var lineStart = true

        fun writeIndentIfNeeded() {
            if (lineStart) {
                repeat(indent) { builder.append(' ') }
                lineStart = false
            }
        }

        fun writeNewline() {
            builder.append('\n')
            lineStart = true
        }

        var index = 0
        while (index < tokens.size) {
            when (val token = tokens[index]) {
                is Token.Text -> {
                    if (!lineStart && !token.value.startsWith('.')) {
                        builder.append(' ')
                    }
                    writeIndentIfNeeded()
                    builder.append(token.value)
                    index++
                }

                Token.OpenParen -> {
                    if (index + 1 < tokens.size && tokens[index + 1] == Token.CloseParen) {
                        writeIndentIfNeeded()
                        builder.append("()")
                        index += 2
                    } else {
                        writeIndentIfNeeded()
                        builder.append('(')
                        indent += INDENT
                        writeNewline()
                        index++
                    }
                }

                Token.CloseParen -> {
                    indent = (indent - INDENT).coerceAtLeast(0)
                    writeNewline()
                    writeIndentIfNeeded()
                    builder.append(')')
                    index++
                }

                Token.Comma -> {
                    writeIndentIfNeeded()
                    builder.append(',')
                    writeNewline()
                    index++
                }

                Token.OpenBrace -> {
                    if (!lineStart) {
                        builder.append(' ')
                    }
                    writeIndentIfNeeded()
                    builder.append('{')
                    indent += INDENT
                    writeNewline()
                    index++
                }

                Token.CloseBrace -> {
                    indent = (indent - INDENT).coerceAtLeast(0)
                    writeNewline()
                    writeIndentIfNeeded()
                    builder.append('}')
                    index++
                }

                Token.Newline -> {
                    val nextIsClosing = index + 1 < tokens.size &&
                        (tokens[index + 1] == Token.CloseBrace || tokens[index + 1] == Token.CloseParen)
                    if (!lineStart && !nextIsClosing) {
                        writeNewline()
                    }
                    index++
                }
            }
        }

        return builder.toString()
    }

    private fun tokenize(code: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var index = 0
        while (index < code.length) {
            when (val char = code[index]) {
                '(' -> {
                    tokens += Token.OpenParen
                    index++
                }

                ')' -> {
                    tokens += Token.CloseParen
                    index++
                }

                ',' -> {
                    tokens += Token.Comma
                    index++
                }

                '{' -> {
                    tokens += Token.OpenBrace
                    index++
                }

                '}' -> {
                    tokens += Token.CloseBrace
                    index++
                }

                '"', '\'' -> {
                    val start = index
                    index = skipLiteral(code, index)
                    tokens += Token.Text(code.substring(start, index))
                }

                else -> {
                    if (char == '\n' || char == '\r') {
                        while (index < code.length && code[index].isWhitespace()) index++
                        tokens += Token.Newline
                    } else if (char.isWhitespace()) {
                        index++
                    } else {
                        val start = index
                        while (index < code.length && !code[index].isStructuralOrWhitespace()) index++
                        tokens += Token.Text(code.substring(start, index))
                    }
                }
            }
        }
        return tokens
    }

    private fun skipLiteral(code: String, start: Int): Int {
        val quote = code[start]
        var index = start + 1
        while (index < code.length) {
            if (code[index] == '\\') {
                index += 2
                continue
            }
            if (code[index] == quote) return index + 1
            index++
        }
        return code.length
    }

    private fun Char.isStructural() =
        this == '(' || this == ')' || this == ',' || this == '{' || this == '}'

    private fun Char.isStructuralOrWhitespace() =
        isStructural() || isWhitespace() || this == '"' || this == '\''

    private sealed interface Token {
        data class Text(val value: String) : Token
        object OpenParen : Token
        object CloseParen : Token
        object Comma : Token
        object OpenBrace : Token
        object CloseBrace : Token
        object Newline : Token
    }
}
