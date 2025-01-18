package io.littlelanguages.bendu.compiler

sealed class Doc {
    data object EmptyDoc : Doc()
    data class VerticalDoc(val docs: List<Doc>) : Doc()
    data class IndentDoc(val docs: List<Doc>) : Doc()
    data class TextDoc(val text: String) : Doc()
    data class PlusDoc(val l: Doc, val r: Doc) : Doc()
    data class PlusPlusDoc(val l: Doc, val sep: Doc, val r: Doc) : Doc()
    data class NestDoc(val offset: Int, val doc: Doc) : Doc()
}

fun text(t: String): Doc = Doc.TextDoc(text = t)

val empty: Doc = Doc.EmptyDoc

val blank: Doc = text("")

val comma: Doc = text(",")

val space: Doc = text(" ")

fun number(n: Int): Doc = text(n.toString())

fun toDoc(doc: Any): Doc = if (doc is Doc) doc else text(doc as String)

fun vcat(docs: List<Any>): Doc = Doc.VerticalDoc(docs = docs.map(::toDoc))

fun indent(docs: List<Any>): Doc = Doc.IndentDoc(docs = docs.map(::toDoc))

fun p(l: Doc, r: Doc): Doc = if (l is Doc.EmptyDoc) r else Doc.PlusDoc(l = l, r = r)

fun pp(l: Doc, r: Doc, sep: Any = space): Doc {
    val separator = toDoc(sep)
    return when {
        separator is Doc.EmptyDoc -> p(l, r)
        separator is Doc.TextDoc && separator.text.isEmpty() -> p(l, r)
        else -> Doc.PlusPlusDoc(l = l, sep = separator, r = r)
    }
}

fun hsep(docs: List<Any>, sep: Any = space): Doc {
    if (docs.isEmpty()) return empty
    val docDocs = docs.map(::toDoc)
    if (docDocs.size == 1) return docDocs[0]
    val (theFirst, allButTheFirst) = docDocs.first() to docDocs.drop(1)
    return allButTheFirst.fold(theFirst) { a, b -> pp(a, b, sep) }
}

fun hcat(docs: List<Any>): Doc {
    if (docs.isEmpty()) return empty
    val docDocs = docs.map(::toDoc)
    if (docDocs.size == 1) return docDocs[0]
    return docDocs.drop(1).fold(docDocs[0]) { a, b -> p(a, b) }
}

fun nest(offset: Int, doc: Doc): Doc = Doc.NestDoc(offset = offset, doc = doc)

fun punctuate(separator: Any, docs: List<Any>): List<Doc> {
    val docDocs = docs.map(::toDoc)
    if (docDocs.size <= 1) return docDocs
    val docSeparator = toDoc(separator)
    return docDocs.dropLast(1).flatMap { listOf(p(it, docSeparator)) } + docDocs.last()
}

fun join(docs: List<Any>, separator: Any = space, lastSeparator: Any? = null): Doc {
    if (docs.isEmpty()) return blank
    val result = mutableListOf<Any>()
    val docSeparator = toDoc(separator)
    docs.forEachIndexed { index, doc ->
        if (index > 0) result.add(if (index == docs.size - 1 && lastSeparator != null) lastSeparator else docSeparator)
        result.add(doc)
    }
    return hcat(result)
}

private fun renderVertically(docs: List<Doc>, leftMargin: Int, offset: Int, writer: java.io.Writer): Int {
    var off = offset
    val newDocs = docs.filter { it !is Doc.EmptyDoc }
    for ((idx, line) in newDocs.withIndex()) {
        val spaces = if (off < leftMargin) " ".repeat(leftMargin - off) else ""
        writer.write(spaces)
        off = renderp(line, leftMargin, off + spaces.length, writer)
        if (idx != newDocs.size - 1) {
            writer.write("\n")
            off = 0
        }
    }
    return off
}

private fun renderp(d: Doc, leftMargin: Int, offset: Int, writer: java.io.Writer): Int {
    return when (d) {
        is Doc.EmptyDoc -> offset
        is Doc.TextDoc -> {
            writer.write(d.text)
            offset + d.text.length
        }

        is Doc.VerticalDoc -> renderVertically(d.docs, leftMargin, offset, writer)
        is Doc.PlusDoc -> renderp(d.l, leftMargin, offset, writer).let { renderp(d.r, leftMargin, it, writer) }
        is Doc.PlusPlusDoc -> {
            if (d.l is Doc.EmptyDoc && d.r is Doc.EmptyDoc) {
                offset
            } else if (d.l is Doc.EmptyDoc) {
                renderp(d.r, leftMargin, offset, writer)
            } else if (d.r is Doc.EmptyDoc) {
                renderp(d.l, leftMargin, offset, writer)
            } else {
                renderp(d.l, leftMargin, offset, writer).let { renderp(d.sep, leftMargin, it, writer) }
                    .let { renderp(d.r, leftMargin, it, writer) }
            }
        }

        is Doc.NestDoc -> {
            val newLeftMargin = leftMargin + d.offset
            val spaces = if (offset < newLeftMargin) " ".repeat(newLeftMargin - offset) else ""
            writer.write(spaces)
            renderp(d.doc, newLeftMargin, newLeftMargin, writer)
        }

        is Doc.IndentDoc -> renderVertically(d.docs, offset, offset, writer)
    }
}

fun render(doc: Doc, writer: java.io.Writer) =
    renderp(doc, 0, 0, writer)
