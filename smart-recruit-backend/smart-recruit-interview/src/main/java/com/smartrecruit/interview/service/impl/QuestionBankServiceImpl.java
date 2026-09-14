package com.smartrecruit.interview.service.impl;

import com.smartrecruit.interview.dto.request.SubmitAnswersRequest;
import com.smartrecruit.interview.dto.response.AssessmentQuestionItem;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.smartrecruit.interview.service.QuestionBankService;

/**
 * 在线测评题库服务。
 *
 * <p>提供三类测评的硬编码试题：编程测试（单选题）、性格测试（Likert量表）、智商测试（逻辑推理）。
 * MVP 阶段为硬编码，后续可迁移至数据库。</p>
 *
 * @since 1.0.0
 */
@Service
public class QuestionBankServiceImpl implements QuestionBankService {

    // ================================================================
    // 编程测试 (type=0) — 10 道单选题
    // ================================================================

    private static final List<AssessmentQuestionItem> CODING_QUESTIONS = List.of(
            q(1, 0, "在Java中，以下哪个关键字用于定义接口？", "single_choice",
                    List.of(opt("A", "extends"), opt("B", "implements"), opt("C", "abstract"), opt("D", "interface")), "D"),
            q(2, 0, "以下哪个不是Java的基本数据类型？", "single_choice",
                    List.of(opt("A", "int"), opt("B", "boolean"), opt("C", "String"), opt("D", "double")), "C"),
            q(3, 0, "Spring框架中，以下哪个注解用于依赖注入？", "single_choice",
                    List.of(opt("A", "@Component"), opt("B", "@Autowired"), opt("C", "@Service"), opt("D", "@Repository")), "B"),
            q(4, 0, "在SQL中，以下哪个关键字用于去除重复行？", "single_choice",
                    List.of(opt("A", "UNIQUE"), opt("B", "DISTINCT"), opt("C", "DIFFERENT"), opt("D", "GROUP")), "B"),
            q(5, 0, "以下哪种数据结构采用FIFO（先进先出）策略？", "single_choice",
                    List.of(opt("A", "栈 Stack"), opt("B", "队列 Queue"), opt("C", "堆 Heap"), opt("D", "树 Tree")), "B"),
            q(6, 0, "在Java中，`==` 和 `equals()` 的区别是什么？", "single_choice",
                    List.of(opt("A", "没有区别"), opt("B", "== 比较引用，equals 比较内容"), opt("C", "== 比较内容，equals 比较引用"), opt("D", "都是比较内容")), "B"),
            q(7, 0, "HTTP状态码 404 表示什么？", "single_choice",
                    List.of(opt("A", "服务器错误"), opt("B", "请求成功"), opt("C", "资源未找到"), opt("D", "未授权")), "C"),
            q(8, 0, "Git中，将本地更改推送到远程仓库使用哪个命令？", "single_choice",
                    List.of(opt("A", "git commit"), opt("B", "git push"), opt("C", "git pull"), opt("D", "git merge")), "B"),
            q(9, 0, "在MySQL中，以下哪个是事务的ACID特性之一？", "single_choice",
                    List.of(opt("A", "快速性 Speed"), opt("B", "原子性 Atomicity"), opt("C", "可读性 Readability"), opt("D", "可用性 Availability")), "B"),
            q(10, 0, "RESTful API 设计原则中，GET 请求通常用于什么操作？", "single_choice",
                    List.of(opt("A", "创建资源"), opt("B", "更新资源"), opt("C", "获取资源"), opt("D", "删除资源")), "C")
    );

    // ================================================================
    // 性格测试 (type=1) — 10 道 Likert 量表
    // ================================================================

    private static final List<AssessmentQuestionItem> PERSONALITY_QUESTIONS = List.of(
            p(1, "我喜欢在团队中担任领导角色", 0),       // 外向性
            p(2, "面对新环境时，我能快速适应并融入", 1),     // 开放性
            p(3, "我习惯提前规划每一天的工作安排", 2),       // 尽责性
            p(4, "我喜欢参加社交活动和结交新朋友", 0),       // 外向性
            p(5, "我对抽象的概念和理论很感兴趣", 1),        // 开放性
            p(6, "我会认真完成自己承诺的事情", 2),           // 尽责性
            p(7, "在团队讨论中，我倾向于主动表达观点", 0),   // 外向性
            p(8, "我喜欢尝试新的工作方法而不是循规蹈矩", 1),  // 开放性
            p(9, "即使没有监督，我也能保持高效工作", 2),      // 尽责性
            p(10, "我愿意倾听不同的意见并调整自己的看法", 1)  // 开放性
    );

    // ================================================================
    // 智商测试 (type=2) — 10 道逻辑推理题
    // ================================================================

    private static final List<AssessmentQuestionItem> IQ_QUESTIONS = List.of(
            q(1, 2, "数列 2, 6, 12, 20, 30, ? 的下一个数字是什么？", "single_choice",
                    List.of(opt("A", "36"), opt("B", "42"), opt("C", "40"), opt("D", "38")), "B"),
            q(2, 2, "如果所有的A都是B，所有的B都是C，那么以下哪个一定正确？", "single_choice",
                    List.of(opt("A", "所有的C都是A"), opt("B", "所有的A都是C"), opt("C", "有些C不是B"), opt("D", "有些A不是C")), "B"),
            q(3, 2, "3, 9, 27, 81, ? 数列的下一个数字是？", "single_choice",
                    List.of(opt("A", "162"), opt("B", "243"), opt("C", "324"), opt("D", "135")), "B"),
            q(4, 2, "五个连续偶数的和是130，最小的偶数是多少？", "single_choice",
                    List.of(opt("A", "20"), opt("B", "22"), opt("C", "24"), opt("D", "26")), "B"),
            q(5, 2, "教室里有30人，18人喜欢数学，15人喜欢语文，10人两门都喜欢，有多少人至少喜欢一门？", "single_choice",
                    List.of(opt("A", "23"), opt("B", "25"), opt("C", "33"), opt("D", "30")), "A"),
            q(6, 2, "1, 1, 2, 3, 5, 8, ? 数列的下一个数字是？", "single_choice",
                    List.of(opt("A", "10"), opt("B", "11"), opt("C", "12"), opt("D", "13")), "D"),
            q(7, 2, "甲乙丙三人中只有一人说了真话。甲说：\"乙在说谎\"，乙说：\"丙在说谎\"，丙说：\"甲和乙都在说谎\"。谁说了真话？", "single_choice",
                    List.of(opt("A", "甲"), opt("B", "乙"), opt("C", "丙"), opt("D", "无法确定")), "B"),
            q(8, 2, "时钟显示3:30，时针和分针之间的角度是多少？", "single_choice",
                    List.of(opt("A", "65°"), opt("B", "75°"), opt("C", "70°"), opt("D", "80°")), "B"),
            q(9, 2, "一个池塘中睡莲每天面积翻一倍，第48天铺满整个池塘，请问第几天铺满一半？", "single_choice",
                    List.of(opt("A", "24天"), opt("B", "47天"), opt("C", "36天"), opt("D", "40天")), "B"),
            q(10, 2, "下列选项中，哪个与其他三个不属于同一类别？", "single_choice",
                    List.of(opt("A", "苹果"), opt("B", "香蕉"), opt("C", "橘子"), opt("D", "胡萝卜")), "D")
    );

    // ================================================================
    // 公共方法
    // ================================================================

    /**
     * 根据测评类型获取试题列表。
     *
     * @param type  0=编程测试, 1=性格测试, 2=智商测试
     * @param count 返回题目数量
     */
    public List<AssessmentQuestionItem> getQuestions(Integer type, int count) {
        List<AssessmentQuestionItem> all = switch (type) {
            case 0 -> CODING_QUESTIONS;
            case 1 -> PERSONALITY_QUESTIONS;
            case 2 -> IQ_QUESTIONS;
            default -> Collections.emptyList();
        };
        if (count <= 0 || count >= all.size()) {
            // 返回防御性拷贝，避免调用方修改影响静态题库
            return deepCopyQuestions(all);
        }
        // shuffle 后取前 count 道（保证每次不同）
        List<AssessmentQuestionItem> shuffled = new java.util.ArrayList<>(all);
        Collections.shuffle(shuffled);
        return deepCopyQuestions(shuffled.subList(0, count));
    }

    /**
     * 深拷贝题目列表，避免调用方（如 stripCorrectAnswers）修改原始题库数据。
     */
    private List<AssessmentQuestionItem> deepCopyQuestions(List<AssessmentQuestionItem> source) {
        List<AssessmentQuestionItem> copies = new java.util.ArrayList<>(source.size());
        for (AssessmentQuestionItem q : source) {
            copies.add(AssessmentQuestionItem.builder()
                    .questionId(q.getQuestionId())
                    .type(q.getType())
                    .questionText(q.getQuestionText())
                    .difficulty(q.getDifficulty())
                    .questionType(q.getQuestionType())
                    .options(q.getOptions() != null ? new java.util.ArrayList<>(q.getOptions()) : null)
                    .correctAnswer(q.getCorrectAnswer())
                    .score(q.getScore())
                    .build());
        }
        return copies;
    }

    /**
     * 评分并返回成绩描述。
     *
     * @param type      测评类型
     * @param questions 使用的试题列表
     * @param answers   候选人提交的答案
     * @return 分数描述，如 "7/10" 或 "外向型"
     */
    public String scoreAnswers(Integer type, List<AssessmentQuestionItem> questions,
                               List<SubmitAnswersRequest.AnswerItem> answers) {
        if (questions == null || answers == null) {
            return type == 1 ? "未提交" : "0/" + (questions != null ? questions.size() : 0);
        }
        return switch (type) {
            case 0, 2 -> scoreMultipleChoice(questions, answers);
            case 1 -> scorePersonality(questions, answers);
            default -> "未知类型";
        };
    }

    /**
     * 生成结果描述（用于 AssessResultVO.resultDescription）。
     */
    public String getResultDescription(Integer type, String score, int total, int correct) {
        if (type == 1) {
            return switch (score) {
                case "外向型" -> "您善于社交沟通，适合需要频繁协作的岗位，在团队中能发挥桥梁作用。";
                case "尽责型" -> "您做事认真负责、有条理，适合需要高度自律和细节把控的工作。";
                case "开放型" -> "您思维活跃、乐于创新，适合需要创造力和灵活性的岗位。";
                case "平衡型" -> "您性格均衡，在不同场景下都能展现出良好的适应能力。";
                default -> "感谢您完成本次性格测评。";
            };
        }
        double rate = total > 0 ? (double) correct / total : 0;
        if (rate >= 0.8) {
            return "成绩优秀！您展现出了出色的能力水平，远超大多数候选人。";
        } else if (rate >= 0.6) {
            return "成绩良好！您具备扎实的基础能力，持续提升将更上一层楼。";
        } else {
            return "本次成绩尚需提升，建议加强相关知识的学习和实践。";
        }
    }

    // ================================================================
    // 私有评分方法
    // ================================================================

    private String scoreMultipleChoice(List<AssessmentQuestionItem> questions,
                                       List<SubmitAnswersRequest.AnswerItem> answers) {
        int correct = 0;
        for (SubmitAnswersRequest.AnswerItem answer : answers) {
            for (AssessmentQuestionItem q : questions) {
                if (q.getQuestionId().equals(answer.getQuestionId())
                        && q.getCorrectAnswer() != null
                        && q.getCorrectAnswer().equalsIgnoreCase(answer.getSelectedAnswer())) {
                    correct++;
                    break;
                }
            }
        }
        return correct + "/" + questions.size();
    }

    private String scorePersonality(List<AssessmentQuestionItem> questions,
                                    List<SubmitAnswersRequest.AnswerItem> answers) {
        int extroversion = 0, openness = 0, conscientiousness = 0;

        for (SubmitAnswersRequest.AnswerItem answer : answers) {
            int value = 0;
            try {
                value = Integer.parseInt(answer.getSelectedAnswer());
            } catch (NumberFormatException ignored) {
                continue;
            }
            for (AssessmentQuestionItem q : questions) {
                if (q.getQuestionId().equals(answer.getQuestionId())) {
                    int dim = (int) q.getScore(); // 复用 score 字段存储维度: 0=外向性,1=开放性,2=尽责性
                    switch (dim) {
                        case 0 -> extroversion += value;
                        case 1 -> openness += value;
                        case 2 -> conscientiousness += value;
                    }
                    break;
                }
            }
        }

        // 取出最高分维度
        int max = Math.max(extroversion, Math.max(openness, conscientiousness));
        // 如果多个维度并列最高，返回平衡型
        int maxCount = 0;
        if (extroversion == max) maxCount++;
        if (openness == max) maxCount++;
        if (conscientiousness == max) maxCount++;

        if (maxCount >= 2) return "平衡型";
        if (extroversion == max) return "外向型";
        if (openness == max) return "开放型";
        return "尽责型";
    }

    // ================================================================
    // 便捷构造方法
    // ================================================================

    private static AssessmentQuestionItem q(int id, int type, String text, String qType,
                                             List<Map<String, String>> options, String answer) {
        return AssessmentQuestionItem.builder()
                .questionId(id).type(type).questionText(text)
                .questionType(qType).options(options)
                .correctAnswer(answer).score(1)
                .build();
    }

    private static AssessmentQuestionItem p(int id, String text, int dimension) {
        return AssessmentQuestionItem.builder()
                .questionId(id).type(1).questionText(text)
                .questionType("likert")
                .options(List.of(
                        opt("1", "非常不同意"),
                        opt("2", "不同意"),
                        opt("3", "中立"),
                        opt("4", "同意"),
                        opt("5", "非常同意")
                ))
                .score(dimension) // 复用 score 存储维度: 0=外向性, 1=开放性, 2=尽责性
                .build();
    }

    private static Map<String, String> opt(String k, String v) {
        return Map.of("key", k, "value", v);
    }
}
