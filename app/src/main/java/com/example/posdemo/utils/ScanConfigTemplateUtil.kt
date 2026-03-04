package com.example.posdemo.utils

import android.content.Context
import org.w3c.dom.Element
import java.io.File
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Scan 配置：XML <-> 模板顺序映射 <-> 明文段反解析
 *
 * 只负责：
 * 1) export XML -> Map
 * 2) assets template XML -> ordered ids
 * 3) Map -> 按模板顺序输出 values
 * 4) values/明文 -> Map -> 输出 XML（property 列表）
 *
 * 不负责：zip/base64/DES
 */
object ScanConfigTemplateUtil {

    // 协议里常见分隔符（和你之前看到的一样）
    private const val FS = '\u001c' // field separator
    private const val RS = '\u001e' // record separator（profilePackages那种）
    private const val DEFAULT_EMPTY = '\u001d' // 模板默认占位

    // -----------------------------
    // 1) 读取模板（assets中的 scanner_setting.xml / scanwedge_scanner_property.xml）
    // -----------------------------

    /**
     * 从 assets 模板里读取 <property id="..."> 的顺序，返回有序 id 列表
     */
    @JvmStatic
    fun loadTemplateIdsFromAssets(context: Context, assetName: String): List<String> {
        context.assets.open(assetName).use { input ->
            return loadTemplateIdsFromStream(input)
        }
    }

    /**
     * 从 InputStream（比如 assets.open 的流）读取模板 property id 顺序
     */
    @JvmStatic
    fun loadTemplateIdsFromStream(templateXml: InputStream): List<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(templateXml)
        val nodes = doc.documentElement.getElementsByTagName("property")
        val ids = ArrayList<String>(nodes.length)

        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? Element ?: continue
            val id = el.getAttribute("id")
            if (id.isNotBlank()) ids.add(id)
        }
        return ids
    }

    // -----------------------------
    // 2) 解析 export XML（你之前的 queryXML 那种） -> Map
    // -----------------------------

    /**
     * 解析导出的扫描配置 XML：
     * - 读取 <property id="xxx">value</property> 到 map
     * - 同时兼容读取 <profileName> <profileEnable> <scanwedgeEnable> 这类标签（如果存在）
     */
    @JvmStatic
    fun parseExportXmlToMap(xmlFile: File): Map<String, String> {
        require(xmlFile.exists()) { "XML file not found: ${xmlFile.absolutePath}" }

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val map = LinkedHashMap<String, String>()

        // 兼容：profileName / profileEnable / scanwedgeEnable
        fun readSingleTag(tag: String) {
            val list = doc.getElementsByTagName(tag)
            if (list != null && list.length > 0) {
                val node = list.item(0)
                val value = node?.firstChild?.nodeValue ?: ""
                map[tag] = value
            }
        }
        readSingleTag("profileName")
        readSingleTag("profileEnable")
        readSingleTag("scanwedgeEnable")

        // property 列表
        val props = doc.getElementsByTagName("property")
        for (i in 0 until props.length) {
            val el = props.item(i) as? Element ?: continue
            val id = el.getAttribute("id")
            if (id.isNullOrBlank()) continue
            val value = el.firstChild?.nodeValue ?: ""
            map[id] = value
        }

        return map
    }

    // -----------------------------
    // 3) Map -> 按模板顺序输出 values（缺省填 \u001d）
    // -----------------------------

    /**
     * 把解析出来的 map 按模板 id 顺序映射成 values 列表
     * 缺失的字段填 DEFAULT_EMPTY（\u001d），与原 app 逻辑一致
     */
    @JvmStatic
    fun mapToOrderedValues(
        templateIds: List<String>,
        valueMap: Map<String, String>,
        defaultValue: String = DEFAULT_EMPTY.toString()
    ): List<String> {
        val values = ArrayList<String>(templateIds.size)
        for (id in templateIds) {
            values.add(valueMap[id] ?: defaultValue)
        }
        return values
    }

    /**
     * 如果你更想要 LinkedHashMap（保留顺序 + 方便调试）
     */
    @JvmStatic
    fun mapToOrderedLinkedMap(
        templateIds: List<String>,
        valueMap: Map<String, String>,
        defaultValue: String = DEFAULT_EMPTY.toString()
    ): LinkedHashMap<String, String> {
        val out = LinkedHashMap<String, String>(templateIds.size)
        for (id in templateIds) {
            out[id] = valueMap[id] ?: defaultValue
        }
        return out
    }

    // -----------------------------
    // 4) 反解析：有序 values / 明文 -> Map
    // -----------------------------

    /**
     * 已知模板 id 顺序 + values 列表 -> Map
     * 多余 values 会忽略；缺少 values 会用 defaultValue 填
     */
    @JvmStatic
    fun orderedValuesToMap(
        templateIds: List<String>,
        values: List<String>,
        defaultValue: String = DEFAULT_EMPTY.toString()
    ): Map<String, String> {
        val out = LinkedHashMap<String, String>(templateIds.size)
        for (i in templateIds.indices) {
            val id = templateIds[i]
            val v = if (i < values.size) values[i] else defaultValue
            out[id] = v
        }
        return out
    }

    /**
     * 从 “scan/scanWedge 协议明文（已解密+解压后的字符串）” 里反解析出：
     * - mode（scan 或 scanWedge）
     * - scanWedge 头部字段（如果有）
     * - values 列表（与模板一一对应的那段）
     *
     * 注意：这里只做“分割和取出 values 段”，不做完整校验。
     */
    data class ParsedPlain(
        val mode: String,
        val profileName: String? = null,
        val scanwedgeEnable: String? = null,
        val profileEnable: String? = null,
        val profilePackagesRaw: String? = null, // 原始 "pkg/val\u001e..." 那段
        val values: List<String>
    )

    @JvmStatic
    fun parsePlainToValues(plain: String): ParsedPlain {
        require(plain.isNotEmpty()) { "plain is empty" }

        // 以 FS 分割，保留空段（limit = -1）
        val parts = plain.split(FS, ignoreCase = false, limit = -1)
        require(parts.isNotEmpty()) { "invalid plain" }

        val head = parts[0]
        return if (head == "scan") {
            // scan\u001c + [values...]
            val values = parts.drop(1).filterNotNull()
            ParsedPlain(
                mode = "scan",
                values = values
            )
        } else if (head == "scanWedge") {
            // scanWedge\u001c profileName\u001c scanwedgeEnable\u001c profileEnable\u001c profilePackages\u001c [values...]
            val profileName = parts.getOrNull(1)
            val scanwedgeEnable = parts.getOrNull(2)
            val profileEnable = parts.getOrNull(3)
            val profilePackages = parts.getOrNull(4)

            val values = if (parts.size > 5) parts.drop(5) else emptyList()

            ParsedPlain(
                mode = "scanWedge",
                profileName = profileName,
                scanwedgeEnable = scanwedgeEnable,
                profileEnable = profileEnable,
                profilePackagesRaw = profilePackages,
                values = values
            )
        } else {
            // 兼容：有些实现会直接没写 head 或 head 变体
            // 这里保守处理：当作 scan 直接从第0段开始就是 values
            ParsedPlain(
                mode = "unknown",
                values = parts
            )
        }
    }

    /**
     * 协议明文 -> (模板id->value) Map
     *
     * 你如果是 scanWedge，需要把 ParsedPlain 里那几个头部字段另存（它们不在模板 values里）
     */
    @JvmStatic
    fun plainToValueMap(templateIds: List<String>, plain: String): Pair<ParsedPlain, Map<String, String>> {
        val parsed = parsePlainToValues(plain)
        val map = orderedValuesToMap(templateIds, parsed.values)
        return parsed to map
    }

    // -----------------------------
    // 5) Map -> 输出 XML（property 列表）
    // -----------------------------

    /**
     * 把 (id->value) map 输出成 XML 字符串：
     * <config>
     *   <property id="xxx">value</property>
     * </config>
     *
     * - 会按 templateIds 顺序输出（更接近原始导出结构）
     * - 你可以自行决定 rootName
     */
    @JvmStatic
    fun valueMapToXmlString(
        templateIds: List<String>,
        valueMap: Map<String, String>,
        rootName: String = "config"
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="utf-8"?>""").append('\n')
        sb.append("<").append(rootName).append(">").append('\n')

        for (id in templateIds) {
            val v = valueMap[id] ?: DEFAULT_EMPTY.toString()
            sb.append("  <property id=\"")
                .append(escapeXmlAttr(id))
                .append("\">")
                .append(escapeXmlText(v))
                .append("</property>")
                .append('\n')
        }

        sb.append("</").append(rootName).append(">").append('\n')
        return sb.toString()
    }

    // -----------------------------
    // 小工具：profilePackages raw 解析（可选）
    // -----------------------------

    /**
     * profilePackagesRaw: "pkg1/val1\u001epkg2/val2\u001e..."
     */
    @JvmStatic
    fun parseProfilePackages(raw: String?): Map<String, String> {
        if (raw.isNullOrEmpty()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        val items = raw.split(RS, ignoreCase = false, limit = -1)
        for (item in items) {
            if (item.isBlank()) continue
            val idx = item.indexOf('/')
            if (idx <= 0) continue
            val pkg = item.substring(0, idx)
            val v = item.substring(idx + 1)
            out[pkg] = v
        }
        return out
    }

    // -----------------------------
    // XML escaping
    // -----------------------------

    private fun escapeXmlText(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    private fun escapeXmlAttr(s: String): String =
        escapeXmlText(s).replace("\"", "&quot;").replace("'", "&apos;")
}