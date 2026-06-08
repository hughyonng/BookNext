package com.booknext.app.util

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 将 TXT 文件转换为标准 EPUB 3 文件。
 * - 按章节正则切分，生成多个 HTML 章节文件
 * - 自动生成 toc.ncx、content.opf、container.xml
 * - 输出到指定的 outputFile（.epub）
 *
 * 使用方法：
 *   val epubFile = TxtToEpubConverter.convert(txtFile, outputFile, bookTitle)
 */
object TxtToEpubConverter {

    // 匹配章节标题：第X章/节、Chapter N、CHAPTER N、卷X、Part N 等
    private val CHAPTER_REGEX = Regex(
        """^[\s　]*""" +
        """(第\s*[〇一二三四五六七八九十百千万零0-9０-９]+\s*[章节卷回篇集部]""" +
        """|Chapter\s+\d+""" +
        """|CHAPTER\s+\d+""" +
        """|Part\s+\d+""" +
        """|卷\s*[〇一二三四五六七八九十百千万零0-9]+""" +
        """|序章|终章|尾声|楔子|番外.*""" +
        """).*$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    )

    data class Chapter(val title: String, val lines: List<String>)

    /**
     * @param txtFile   源 TXT 文件
     * @param outputFile 目标 EPUB 文件路径（会被覆盖）
     * @param bookTitle  书名
     * @param bookId     用于 EPUB unique-identifier
     * @return 生成成功返回 outputFile，失败抛出异常
     */
    fun convert(
        txtFile: File,
        outputFile: File,
        bookTitle: String,
        bookId: String = txtFile.nameWithoutExtension,
    ): File {
        val lines = EncodingDetector.readLines(txtFile)
        val chapters = splitChapters(lines, bookTitle)

        outputFile.parentFile?.mkdirs()
        ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
            writeMimetype(zip)
            writeContainerXml(zip)
            writeContentOpf(zip, bookId, bookTitle, chapters)
            writeTocNcx(zip, bookId, bookTitle, chapters)
            writeNav(zip, bookTitle, chapters)
            chapters.forEachIndexed { idx, chapter ->
                writeChapterHtml(zip, idx, chapter)
            }
            writeCss(zip)
        }
        return outputFile
    }

    // ── 章节切分 ────────────────────────────────────────────────

    private fun splitChapters(lines: List<String>, bookTitle: String): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        var currentTitle = bookTitle
        var currentLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (CHAPTER_REGEX.matches(trimmed) && trimmed.length <= 60) {
                // 保存上一章
                if (currentLines.any { it.isNotBlank() }) {
                    chapters.add(Chapter(currentTitle, currentLines.toList()))
                }
                currentTitle = trimmed
                currentLines = mutableListOf()
            } else {
                currentLines.add(line)
            }
        }
        // 最后一章
        if (currentLines.any { it.isNotBlank() }) {
            chapters.add(Chapter(currentTitle, currentLines.toList()))
        }

        // 如果一个章节都没识别到，整本书作为一章
        if (chapters.isEmpty()) {
            chapters.add(Chapter(bookTitle, lines))
        }

        return chapters
    }

    // ── ZIP 写入各文件 ───────────────────────────────────────────

    /** mimetype 必须是第一个条目且不压缩 */
    private fun writeMimetype(zip: ZipOutputStream) {
        val entry = ZipEntry("mimetype").apply { method = ZipEntry.STORED }
        val data = "application/epub+zip".toByteArray(Charsets.UTF_8)
        entry.size = data.size.toLong()
        entry.compressedSize = data.size.toLong()
        entry.crc = java.util.zip.CRC32().apply { update(data) }.value
        zip.putNextEntry(entry)
        zip.write(data)
        zip.closeEntry()
    }

    private fun writeContainerXml(zip: ZipOutputStream) {
        zip.putNextEntry(ZipEntry("META-INF/container.xml"))
        zip.write("""
            <?xml version="1.0" encoding="UTF-8"?>
            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
              <rootfiles>
                <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
              </rootfiles>
            </container>
        """.trimIndent().toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun writeContentOpf(
        zip: ZipOutputStream,
        bookId: String,
        bookTitle: String,
        chapters: List<Chapter>,
    ) {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<package version="3.0" xmlns="http://www.idpf.org/2007/opf" unique-identifier="uid">""")
        sb.appendLine("""  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">""")
        sb.appendLine("""    <dc:identifier id="uid">$bookId</dc:identifier>""")
        sb.appendLine("""    <dc:title>${escapeXml(bookTitle)}</dc:title>""")
        sb.appendLine("""    <dc:language>zh-CN</dc:language>""")
        sb.appendLine("""    <meta property="dcterms:modified">${java.time.Instant.now().toString().take(19)}Z</meta>""")
        sb.appendLine("""  </metadata>""")
        sb.appendLine("""  <manifest>""")
        sb.appendLine("""    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>""")
        sb.appendLine("""    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>""")
        sb.appendLine("""    <item id="css" href="style.css" media-type="text/css"/>""")
        chapters.forEachIndexed { idx, _ ->
            sb.appendLine("""    <item id="ch${idx}" href="chapter${idx}.xhtml" media-type="application/xhtml+xml"/>""")
        }
        sb.appendLine("""  </manifest>""")
        sb.appendLine("""  <spine toc="ncx">""")
        sb.appendLine("""    <itemref idref="nav"/>""")
        chapters.forEachIndexed { idx, _ ->
            sb.appendLine("""    <itemref idref="ch${idx}"/>""")
        }
        sb.appendLine("""  </spine>""")
        sb.appendLine("""</package>""")

        zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
        zip.write(sb.toString().toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun writeTocNcx(
        zip: ZipOutputStream,
        bookId: String,
        bookTitle: String,
        chapters: List<Chapter>,
    ) {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<ncx version="2005-1" xmlns="http://www.daisy.org/z3986/2005/ncx/">""")
        sb.appendLine("""  <head><meta name="dtb:uid" content="$bookId"/></head>""")
        sb.appendLine("""  <docTitle><text>${escapeXml(bookTitle)}</text></docTitle>""")
        sb.appendLine("""  <navMap>""")
        chapters.forEachIndexed { idx, ch ->
            sb.appendLine("""    <navPoint id="np${idx}" playOrder="${idx + 1}">""")
            sb.appendLine("""      <navLabel><text>${escapeXml(ch.title)}</text></navLabel>""")
            sb.appendLine("""      <content src="chapter${idx}.xhtml"/>""")
            sb.appendLine("""    </navPoint>""")
        }
        sb.appendLine("""  </navMap>""")
        sb.appendLine("""</ncx>""")

        zip.putNextEntry(ZipEntry("OEBPS/toc.ncx"))
        zip.write(sb.toString().toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun writeNav(
        zip: ZipOutputStream,
        bookTitle: String,
        chapters: List<Chapter>,
    ) {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<!DOCTYPE html>""")
        sb.appendLine("""<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">""")
        sb.appendLine("""<head><meta charset="UTF-8"/><title>${escapeXml(bookTitle)}</title></head>""")
        sb.appendLine("""<body><nav epub:type="toc"><ol>""")
        chapters.forEachIndexed { idx, ch ->
            sb.appendLine("""  <li><a href="chapter${idx}.xhtml">${escapeXml(ch.title)}</a></li>""")
        }
        sb.appendLine("""</ol></nav></body></html>""")

        zip.putNextEntry(ZipEntry("OEBPS/nav.xhtml"))
        zip.write(sb.toString().toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun writeChapterHtml(zip: ZipOutputStream, idx: Int, chapter: Chapter) {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<!DOCTYPE html>""")
        sb.appendLine("""<html xmlns="http://www.w3.org/1999/xhtml">""")
        sb.appendLine("""<head>""")
        sb.appendLine("""  <meta charset="UTF-8"/>""")
        sb.appendLine("""  <title>${escapeXml(chapter.title)}</title>""")
        sb.appendLine("""  <link rel="stylesheet" type="text/css" href="style.css"/>""")
        sb.appendLine("""</head>""")
        sb.appendLine("""<body>""")
        sb.appendLine("""  <h2 class="chapter-title">${escapeXml(chapter.title)}</h2>""")

        // 将每行转为 <p>，空行合并跳过连续多个
        var prevBlank = false
        for (line in chapter.lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (!prevBlank) sb.appendLine("""  <p class="blank"> </p>""")
                prevBlank = true
            } else {
                sb.appendLine("""  <p>${escapeXml(trimmed)}</p>""")
                prevBlank = false
            }
        }

        sb.appendLine("""</body>""")
        sb.appendLine("""</html>""")

        zip.putNextEntry(ZipEntry("OEBPS/chapter${idx}.xhtml"))
        zip.write(sb.toString().toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    /**
     * 基础样式：Readium 会用自己的 CSS 覆盖，但保留这个作为 fallback
     * 以及让章节标题样式在 TOC 展示时稍好看
     */
    private fun writeCss(zip: ZipOutputStream) {
        zip.putNextEntry(ZipEntry("OEBPS/style.css"))
        zip.write("""
            body { margin: 0; padding: 0; }
            p { margin: 0.3em 0; text-indent: 2em; }
            p.blank { margin: 0; text-indent: 0; }
            h2.chapter-title { text-align: center; margin: 1em 0 0.5em; }
        """.trimIndent().toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}