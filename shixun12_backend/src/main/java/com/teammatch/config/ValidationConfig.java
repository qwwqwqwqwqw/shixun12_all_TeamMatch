package com.teammatch.config;

import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

/**
 * 互评内容校验配置类
 * 用于 M5-2 互评提交内容基础合法性校验
 */
@Configuration
public class ValidationConfig {

    /**
     * 基础违规词列表
     * P0 阶段使用硬编码，P1 阶段可以从配置文件或数据库读取
     * 原则：只过滤完整的脏话组合词，不过滤单字以避免误伤正常词汇（如"操作"、"操场"）
     */
    private static final Set<String> VIOLATION_WORDS = Set.of(
        // 脏话类（常见骂人词汇及变体）
        "傻逼", "傻b", "傻比", "煞笔", "沙比", "sb", "shabi", "傻13", "傻吊",
        "傻逼玩意", "傻卵", "傻缺", "傻叉", "傻屄", "傻狗",
        "垃圾", "辣鸡", "拉圾", "垃圾玩意",
        "废物", "废材", "饭桶",
        "白痴", "白吃", "智障", "弱智", "脑残", "脑瘫", "智商低", "没脑子",

        // 带"妈"的脏话
        "草泥马", "cnm", "操你妈", "你妈", "nmsl", "你妈死了", "你妈逼",
        "妈的", "他妈的", "tmd", "妈卖批", "mmp", "他妈", "尼玛", "泥马",

        // 蠢/混蛋类
        "蠢货", "蠢驴", "蠢猪", "蠢材", "蠢蛋",
        "混蛋", "王八蛋", "龟儿子",

        // 狗相关脏话
        "狗东西", "狗日的", "狗屎", "狗杂种", "狗腿子", "狗都不如",
        "闭上你的狗嘴",

        // 你大爷/孙子类
        "你大爷", "你大爷的", "给爷", "孙子",

        // 英文脏话
        "fuck", "shit", "damn", "bitch", "ass", "asshole", "bastard", "motherfucker",
        "son of bitch", "bullshit", "crap", "piss", "dick", "cock",

        // 性相关脏话（完整组合）
        "操你", "日你", "干你", "艹你", "草你",
        "鸡巴", "几把", "jb",
        "牛逼", "牛b", "nb", "niubi", "装逼",
        "我操", "我草", "我艹", "卧槽",

        // 人身攻击类
        "去死", "死全家", "死妈", "死爹", "死爸",
        "滚蛋", "滚犊子", "滚开", "滚远点",
        "找死", "该死", "活该死",
        "闭嘴", "住口",

        // 歧视类
        "残废", "瘸子", "瞎子", "聋子", "哑巴", "傻子",
        "神经病", "精神病", "有病", "脑子有病",

        // 侮辱性词汇（完整组合）
        "贱人", "贱货", "下贱", "贱逼", "贱骨头",
        "婊子", "绿茶婊", "表子",
        "人渣", "败类", "畜生", "禽兽", "走狗",

        // 其他
        "吃屎", "喂狗"
    );

    private static final Set<String> POSITIVE_TAGS = Set.of(
        "沟通积极", "按时交付", "技术可靠", "责任心强"
    );

    private static final Set<String> NEGATIVE_TAGS = Set.of(
        "沟通差", "延期", "质量低", "失联"
    );

    /**
     * 检查文本是否包含违规词
     * 使用简单字符串包含检查，大小写不敏感
     *
     * @param text 待检查文本
     * @return 是否包含违规词
     */
    public boolean containsViolation(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        return VIOLATION_WORDS.stream()
                .anyMatch(lowerText::contains);
    }

    public boolean containsAnyValidPositiveTag(List<String> tags) {
        return containsAnyValidTag(tags, POSITIVE_TAGS);
    }

    public boolean allPositiveTagsValid(List<String> tags) {
        return allTagsValid(tags, POSITIVE_TAGS);
    }

    public boolean containsAnyValidNegativeTag(List<String> tags) {
        return containsAnyValidTag(tags, NEGATIVE_TAGS);
    }

    public boolean allNegativeTagsValid(List<String> tags) {
        return allTagsValid(tags, NEGATIVE_TAGS);
    }

    private boolean containsAnyValidTag(List<String> tags, Set<String> whitelist) {
        if (tags == null || tags.isEmpty()) {
            return false;
        }

        return tags.stream()
                .anyMatch(whitelist::contains);
    }

    private boolean allTagsValid(List<String> tags, Set<String> whitelist) {
        if (tags == null || tags.isEmpty()) {
            return false;
        }

        return tags.stream()
                .allMatch(whitelist::contains);
    }
}
