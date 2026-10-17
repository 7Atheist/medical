-- =====================================================================
-- 自助式老年综合评估对话系统 - 题目级种子数据（依据 WS/T 888—2026 附录B 重建）
-- scale_id 与 seed.sql 插入顺序一致：1=表B.1 ... 21=表B.21
-- 全部21套量表均提供完整题目，计分/阈值与国标一致，报告不再出现"未配置"。
-- =====================================================================
USE `geriatric_assessment`;

-- ---------------------------------------------------------------------
-- 表B.1 基本日常生活活动能力评估量表 (scale_id=1)，满分100
-- 分值按国标：进食10/5/0；洗澡5/0；修饰5/0；穿衣10/5/0；控制大便10/5/0；
--            控制小便10/5/0；如厕10/5/0；床椅转移15/10/5/0；平地行走15/10/5/0；上下楼10/5/0
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(1,'CHOICE','进食：能否独立用筷子(勺子/叉子)取食物、咀嚼吞咽？','咱们先聊聊吃饭。您现在能自己吃饭吗？比如用筷子或勺子把饭送到嘴里、自己咀嚼吞咽，需不需要别人喂或帮忙呀？',JSON_ARRAY(JSON_OBJECT('value','0','label','需极大帮助或完全依赖他人，或留置胃管','score',0),JSON_OBJECT('value','5','label','需部分帮助','score',5),JSON_OBJECT('value','10','label','可独立进食','score',10)),NULL,1,1),
(1,'CHOICE','洗澡：能否独立完成进出浴室、穿脱衣裤、洗浴全身？','洗澡这件事，您能自己完成吗？比如自己进浴室、擦洗全身、洗头，需不需要别人帮忙？',JSON_ARRAY(JSON_OBJECT('value','0','label','洗澡过程需要他人帮助','score',0),JSON_OBJECT('value','5','label','准备好洗澡水后可独立完成','score',5)),NULL,2,1),
(1,'CHOICE','修饰：能否独立完成洗脸、刷牙、梳头、刮脸？','那刷牙、洗脸、梳头这些事，您自己都能做吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','需他人帮助','score',0),JSON_OBJECT('value','5','label','可独立完成','score',5)),NULL,3,1),
(1,'CHOICE','穿(脱)衣：能否独立穿脱衣服、系扣子、拉拉链、穿脱鞋袜、系鞋带？','穿衣服、脱衣服，您自己能行吗？包括扣扣子、拉拉链、穿脱鞋袜、系鞋带这些？',JSON_ARRAY(JSON_OBJECT('value','0','label','需极大帮助或完全依赖他人','score',0),JSON_OBJECT('value','5','label','需部分帮助','score',5),JSON_OBJECT('value','10','label','可独立完成','score',10)),NULL,4,1),
(1,'CHOICE','控制大便：能否自主控制排便？','大便方面，您能自己控制住吗？有没有偶尔失禁或完全失控的情况？',JSON_ARRAY(JSON_OBJECT('value','0','label','完全失控','score',0),JSON_OBJECT('value','5','label','偶尔失控，或需要他人提示','score',5),JSON_OBJECT('value','10','label','可控制大便','score',10)),NULL,5,1),
(1,'CHOICE','控制小便：能否自主控制排尿？','小便方面呢，能自己控制住吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','完全失控，或留置导尿管','score',0),JSON_OBJECT('value','5','label','偶尔失控，或需要他人提示','score',5),JSON_OBJECT('value','10','label','可控制小便','score',10)),NULL,6,1),
(1,'CHOICE','如厕：能否独立完成去厕所、解衣、擦净、整理、冲水？','上厕所这件事，您能自己去吗？包括自己解衣、擦净、整理、冲水这些？',JSON_ARRAY(JSON_OBJECT('value','0','label','需极大帮助或完全依赖他人','score',0),JSON_OBJECT('value','5','label','需部分帮助','score',5),JSON_OBJECT('value','10','label','可独立完成','score',10)),NULL,7,1),
(1,'CHOICE','床椅转移：能否独立完成床与床旁椅之间的转移？','从床上坐起来坐到椅子上、再从椅子上回床上，您自己能完成吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','完全依赖他人','score',0),JSON_OBJECT('value','5','label','需极大帮助','score',5),JSON_OBJECT('value','10','label','需部分帮助','score',10),JSON_OBJECT('value','15','label','可独立完成','score',15)),NULL,8,1),
(1,'CHOICE','平地行走：能否在平地上独立行走45米？','在平地上自己走一段路，比如在屋里走几步，您能行吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','完全依赖他人','score',0),JSON_OBJECT('value','5','label','需极大帮助','score',5),JSON_OBJECT('value','10','label','需部分帮助(如拄拐/搀扶)','score',10),JSON_OBJECT('value','15','label','可独立行走45米','score',15)),NULL,9,1),
(1,'CHOICE','上下楼梯：能否独立连续上下10~15个台阶？','上下楼梯呢，您自己能连续上下一段台阶吗？需不需要扶着或别人帮忙？',JSON_ARRAY(JSON_OBJECT('value','0','label','需极大帮助或完全依赖他人','score',0),JSON_OBJECT('value','5','label','需部分帮助','score',5),JSON_OBJECT('value','10','label','可独立上下楼梯','score',10)),NULL,10,1);

-- ---------------------------------------------------------------------
-- 表B.2 工具性日常生活活动能力量表 (scale_id=2)，满分8，各项1或0分
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(2,'CHOICE','上街购物：能否独立完成购物需求？','您平时能自己上街买菜、买东西吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','需陪伴或完全不上街购物','score',0),JSON_OBJECT('value','1','label','独立完成所有购物需求','score',1)),NULL,1,1),
(2,'CHOICE','使用交通工具：能否独立乘坐公交/自驾/出租车并安排路线？','您出门坐车方便吗？能自己坐公交、打车或自己开车吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','仅能在他人陪伴下外出或不能外出','score',0),JSON_OBJECT('value','1','label','能独立乘坐公共交通工具或独自驾车','score',1)),NULL,2,1),
(2,'CHOICE','食物烹调：能否独立计划、烹煮、摆好一顿饭菜？','您能自己做一顿饭吗？从买菜做饭到端上桌？',JSON_ARRAY(JSON_OBJECT('value','0','label','需别人备料/帮做或只会加热现成饭菜','score',0),JSON_OBJECT('value','1','label','能独立计划、烹煮并摆好一顿饭菜','score',1)),NULL,3,1),
(2,'CHOICE','家务维持：能否独立完成较繁重或简单的家务？','家里扫地、擦桌、铺床、叠被这些家务，您自己能做吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','完全不能做家务','score',0),JSON_OBJECT('value','1','label','能做较繁重或较简单的家务','score',1)),NULL,4,1),
(2,'CHOICE','洗衣服：能否自己清洗衣物？','洗衣服这件事，您自己能洗吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','所有衣物须由别人洗及晾晒','score',0),JSON_OBJECT('value','1','label','自己清洗所有或大部分衣物','score',1)),NULL,5,1),
(2,'CHOICE','使用电话：能否独立使用电话(含查号、拨号)？','您能自己打电话、接电话吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','完全不会使用电话或不适用','score',0),JSON_OBJECT('value','1','label','能独立使用电话(含拨号/查号)','score',1)),NULL,6,1),
(2,'CHOICE','服用药物：能否自己按时、按量服用药物？','您吃的药，能自己按时按量吃吗？需不需要别人提醒或帮摆药？',JSON_ARRAY(JSON_OBJECT('value','0','label','需提醒/协助或不能自己服药','score',0),JSON_OBJECT('value','1','label','能自己在正确时间服用正确药物','score',1)),NULL,7,1),
(2,'CHOICE','处理财务：能否独立处理财务(银行往来等)？','家里的钱、账，您自己能管吗？比如去银行办事情？',JSON_ARRAY(JSON_OBJECT('value','0','label','不能处理财务或需协助银行往来','score',0),JSON_OBJECT('value','1','label','可独立处理财务','score',1)),NULL,8,1);

-- ---------------------------------------------------------------------
-- 表B.3 简易体能状况量表 (scale_id=3)，满分12（平衡0~4 + 步速0~4 + 起坐0~4）
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(3,'CHOICE','平衡测试：原地并脚、半足距、全足距站立各10秒，合计得分？','接下来测一下您的平衡能力。请您(或家人帮忙看着)依次做：①双脚并拢站10秒；②一脚脚跟贴另一脚大脚趾站10秒；③两脚前后脚跟对脚尖站10秒。能坚持住几项？',JSON_ARRAY(JSON_OBJECT('value','0','label','三项均不能完成/不能站立','score',0),JSON_OBJECT('value','1','label','只能完成并脚站立10秒','score',1),JSON_OBJECT('value','2','label','能完成并脚+半足距站立10秒','score',2),JSON_OBJECT('value','3','label','半足距不足10秒，但能完成并脚与半足距','score',3),JSON_OBJECT('value','4','label','三项(并脚/半足距/全足距)均站立满10秒','score',4)),NULL,1,1),
(3,'CHOICE','4米步速：计时走完4米所需时间？','请您(或家人秒表计时)按平常速度走完约4米的路，看看用了多长时间？',JSON_ARRAY(JSON_OBJECT('value','0','label','不能完成','score',0),JSON_OBJECT('value','1','label','大于8.70秒','score',1),JSON_OBJECT('value','2','label','6.21~8.70秒','score',2),JSON_OBJECT('value','3','label','4.82~6.20秒','score',3),JSON_OBJECT('value','4','label','小于4.82秒','score',4)),NULL,2,1),
(3,'CHOICE','5次起坐试验：双手抱胸，尽可能快地起立坐下5次所需时间？','请坐在椅子上，双手抱在胸前，尽快起立、坐下5次，看看用了多长时间？',JSON_ARRAY(JSON_OBJECT('value','0','label','大于60秒或不能完成','score',0),JSON_OBJECT('value','1','label','16.70~60秒','score',1),JSON_OBJECT('value','2','label','13.70~16.69秒','score',2),JSON_OBJECT('value','3','label','11.20~13.69秒','score',3),JSON_OBJECT('value','4','label','小于等于11.19秒','score',4)),NULL,3,1);

-- ---------------------------------------------------------------------
-- 表B.4 视力简易评估表 (scale_id=4)，5级自评
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(4,'CHOICE','请选择最符合您目前视力状况的一项(可佩戴眼镜评估)','咱们聊一下视力。戴上您平时戴的眼镜后，您现在看东西是什么情况呢？',JSON_ARRAY(JSON_OBJECT('value','0','label','没有视力，眼睛不能跟随物体移动','score',0),JSON_OBJECT('value','1','label','辨认物体困难，只能看到光、颜色和形状','score',1),JSON_OBJECT('value','2','label','视力有限，看不清报纸大标题，但能辨认物体','score',2),JSON_OBJECT('value','3','label','能看清大字体，但看不清书报上的标准字体','score',3),JSON_OBJECT('value','4','label','能看清书报上的标准字体','score',4)),NULL,1,1);

-- ---------------------------------------------------------------------
-- 表B.5 视觉功能简易评估表 (scale_id=5)，3项(0=是,1=否)
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(5,'CHOICE','阅读、行走和看电视时，是否觉得吃力？','您平时看书、走路、看电视的时候，会觉得眼睛吃力吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','是，觉得吃力','score',0),JSON_OBJECT('value','1','label','否，不吃力','score',1)),NULL,1,1),
(5,'CHOICE','看东西时是否觉得有东西遮挡或视物有缺损？','您看东西时，有没有觉得像有什么东西挡着、或者有地方看不清(视物缺损)？',JSON_ARRAY(JSON_OBJECT('value','0','label','是','score',0),JSON_OBJECT('value','1','label','否','score',1)),NULL,2,1),
(5,'CHOICE','看东西时是否出现实物变形、扭曲？','您看东西时，东西会不会变形、变扭曲？',JSON_ARRAY(JSON_OBJECT('value','0','label','是','score',0),JSON_OBJECT('value','1','label','否','score',1)),NULL,3,1);

-- ---------------------------------------------------------------------
-- 表B.6 听力简易评估表 (scale_id=6)，5级自评
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(6,'CHOICE','请选择最符合您目前听力状况的一项(可佩戴助听器评估)','咱们再聊聊听力。戴上您平时用的助听器后，您现在听声音是什么情况呢？',JSON_ARRAY(JSON_OBJECT('value','0','label','完全听不见','score',0),JSON_OBJECT('value','1','label','讲话者大声说话或说得很慢，才能部分听见','score',1),JSON_OBJECT('value','2','label','正常交流有些困难，需在安静环境或大声说话才能听到','score',2),JSON_OBJECT('value','3','label','轻声说话或说话距离超过2米时听不清','score',3),JSON_OBJECT('value','4','label','可正常交谈，能听到电视、电话、门铃声音','score',4)),NULL,1,1);

-- ---------------------------------------------------------------------
-- 表B.7 耳语试验 (scale_id=7)，能否正确重复≥3个词语
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(7,'CHOICE','耳语试验：能否正确重复听到的4个词语中的至少3个？','请家人或旁人站您身后约一臂远，轻声说4个日常词语(如"金鱼、自行车、花园、黄色")，您能正确重复出其中至少3个吗？两侧耳朵都测一下。',JSON_ARRAY(JSON_OBJECT('value','0','label','否，无法正确重复3个及以上词语','score',0),JSON_OBJECT('value','1','label','是，能正确重复3个及以上词语','score',1)),NULL,1,1);

-- ---------------------------------------------------------------------
-- 表B.8 认知功能评估量表 (scale_id=8)，3词回忆(每词1分)+画钟(2分)，满分5
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(8,'CHOICE','记住并回忆第1个词：皮球（请先记住三个词：皮球、国旗、树木）','咱们测一下记忆力。请您记住三个词：皮球、国旗、树木。现在，请您说出第一个词(皮球)还记得吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','记不住','score',0),JSON_OBJECT('value','1','label','记住了/能说出','score',1)),NULL,1,1),
(8,'CHOICE','回忆第2个词：国旗','第二个词(国旗)还记得吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','记不住','score',0),JSON_OBJECT('value','1','label','记住了/能说出','score',1)),NULL,2,1),
(8,'CHOICE','回忆第3个词：树木','第三个词(树木)还记得吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','记不住','score',0),JSON_OBJECT('value','1','label','记住了/能说出','score',1)),NULL,3,1),
(8,'CHOICE','画钟试验：是否能在纸上画出正确的钟(数字标注正确且指针时间正确)？','请您在一张白纸上画一个圆圆的钟表，标上12个数字，再画出"十点一刻"的时针和分针。您画对了吗？(数字标注和指针时间都正确才算对)',JSON_ARRAY(JSON_OBJECT('value','0','label','画错/未画出','score',0),JSON_OBJECT('value','2','label','画正确','score',2)),NULL,4,1);

-- ---------------------------------------------------------------------
-- 表B.9 居家环境筛查表 (scale_id=9)，14项(0=否,1=是)
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(9,'CHOICE','通道是否无杂乱物品？','家里走动的地方(通道)干净没有杂物吧？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,1,1),
(9,'CHOICE','地板状况是否良好？','家里地板平整完好，没有破损吧？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,2,1),
(9,'CHOICE','地板是否防滑？','家里的地面防滑吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,3,1),
(9,'CHOICE','地板上是否有固定的防滑垫？','地上有固定住的防滑垫吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,4,1),
(9,'CHOICE','灯的亮度是否能够让您看清东西？','家里灯光够亮、能看清东西吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,5,1),
(9,'CHOICE','在床上开关灯是否方便？','您躺在床上开关灯方便吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,6,1),
(9,'CHOICE','晚上外边的路灯、楼道的灯照明是否良好？','晚上外面路灯、楼道灯亮不亮？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,7,1),
(9,'CHOICE','淋浴室和浴池旁是否有扶手？','浴室/淋浴旁有扶手吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,8,1),
(9,'CHOICE','在浴池和浴室是否有固定的防滑垫？','浴池/浴室里有固定防滑垫吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,9,1),
(9,'CHOICE','厕所是否接近浴室？','厕所离浴室近吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,10,1),
(9,'CHOICE','室内的楼梯旁是否都有可用的扶手？','家里室内楼梯旁有扶手吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,11,1),
(9,'CHOICE','室外的楼梯旁是否都有可用的扶手？','外面楼梯旁有扶手吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,12,1),
(9,'CHOICE','楼梯的边缘是否清晰？','楼梯边缘清晰、容易看清吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,13,1),
(9,'CHOICE','房屋周围的路况是否良好？','家周围的路好走吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,14,1);

-- ---------------------------------------------------------------------
-- 表B.10 FRAIL衰弱评估量表 (scale_id=10) —— 初筛量表，5条(是=1)
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(10,'CHOICE','过去4周内大部分时间或所有时间是否感到疲乏？','咱们先聊几个简单的小问题。最近这4个星期，您是不是大部分时间都觉得很累、提不起劲？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,1,1),
(10,'CHOICE','在不用辅助工具、不用别人帮助的情况下，中途不休息爬1层楼梯是否有困难？','您能不扶不歇、一口气爬一层楼梯吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否，无困难','score',0),JSON_OBJECT('value','1','label','是，有困难','score',1)),NULL,2,1),
(10,'CHOICE','在不用辅助工具、不用别人帮助的情况下，走完100米(约一个街区)是否较困难？','您能不扶不歇走完大概100米吗？会觉得吃力吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否，不困难','score',0),JSON_OBJECT('value','1','label','是，较困难','score',1)),NULL,3,1),
(10,'CHOICE','医生是否曾告诉您存在5种以上如下疾病(高血压、糖尿病、心脏病、卒中、肿瘤、心衰、哮喘、关节炎、慢阻肺、肾病、心绞痛等)？','医生有没有跟您说过，您同时患有好几种慢性病，比如超过5种？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,4,1),
(10,'CHOICE','1年或更短时间内是否出现体重下降≥5%？','最近1年内，您的体重有没有明显下降，比如下降超过5%？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,5,1);

-- ---------------------------------------------------------------------
-- 表B.11 尿失禁问卷表(简表) (scale_id=11)，漏尿频次+漏尿量+影响程度
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(11,'CHOICE','漏尿频次(最近4周)','下面聊一下漏尿的情况。最近4个星期，您漏尿(尿失禁)大概多久发生一次？',JSON_ARRAY(JSON_OBJECT('value','0','label','从不漏尿','score',0),JSON_OBJECT('value','1','label','大约每周1次或更少','score',1),JSON_OBJECT('value','2','label','每周2~3次','score',2),JSON_OBJECT('value','3','label','大约每天1次','score',3),JSON_OBJECT('value','4','label','每天多于1次','score',4),JSON_OBJECT('value','5','label','一直漏尿','score',5)),NULL,1,1),
(11,'CHOICE','漏尿量(每次漏尿多少)','每次漏的尿量大概是多少？',JSON_ARRAY(JSON_OBJECT('value','0','label','没有漏尿','score',0),JSON_OBJECT('value','2','label','少量(滴湿内裤)','score',2),JSON_OBJECT('value','4','label','中等量(湿透内裤/外裤)','score',4),JSON_OBJECT('value','6','label','大量(流到地上)','score',6)),NULL,2,1),
(11,'CHOICE','漏尿对日常生活的影响程度(0~10分)','漏尿对您日常生活的影响有多大？请用0到10分表示，0分是毫无影响，10分是影响极重。',JSON_ARRAY(JSON_OBJECT('value','0','label','0分 无影响','score',0),JSON_OBJECT('value','1','label','1分','score',1),JSON_OBJECT('value','2','label','2分','score',2),JSON_OBJECT('value','3','label','3分','score',3),JSON_OBJECT('value','4','label','4分','score',4),JSON_OBJECT('value','5','label','5分','score',5),JSON_OBJECT('value','6','label','6分','score',6),JSON_OBJECT('value','7','label','7分','score',7),JSON_OBJECT('value','8','label','8分','score',8),JSON_OBJECT('value','9','label','9分','score',9),JSON_OBJECT('value','10','label','10分 影响极重','score',10)),NULL,3,1),
(11,'OPEN','漏尿发生的时点(可多选，用于了解类型)','您通常在什么时候漏尿呢？(如：到厕所前、咳嗽打喷嚏时、睡着时、活动运动时、小便结束穿衣时、无明显原因、一直漏尿等，如实说一下即可)',JSON_ARRAY(),NULL,4,1);

-- ---------------------------------------------------------------------
-- 表B.12 便秘症状评估表 (scale_id=12)，提示便秘风险
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(12,'CHOICE','您是否受便秘困扰？','您平时有便秘的困扰吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,1,1),
(12,'CHOICE','是否有排便次数减少(每周少于3次)？','您一般一周排便几次？少于3次吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否，每周≥3次','score',0),JSON_OBJECT('value','1','label','是，每周少于3次','score',1)),NULL,2,1),
(12,'CHOICE','是否有排便费力、排便不尽感或需手法辅助排便？','排便的时候，会不会觉得费力、排不干净，或者要用手辅助才能排出？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,3,1),
(12,'OPEN','大便的性状描述(供医务人员判断)','您的大便一般是什么样子？(如干硬呈球状、像腊肠、表面光滑柔软、糊状或水样等，如实描述一下即可)',JSON_ARRAY(),NULL,4,1);

-- ---------------------------------------------------------------------
-- 表B.13 阿森斯失眠量表 (scale_id=13) —— 初筛量表，8项各0~3分
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(13,'CHOICE','入睡时间(关灯后到睡着的时间)','下面聊聊您的睡眠。您躺下关灯后，要多久才能睡着？',JSON_ARRAY(JSON_OBJECT('value','0','label','没问题','score',0),JSON_OBJECT('value','1','label','轻微延迟','score',1),JSON_OBJECT('value','2','label','显著延迟','score',2),JSON_OBJECT('value','3','label','延迟严重或没有睡觉','score',3)),NULL,1,1),
(13,'CHOICE','夜间苏醒','睡着以后，您夜里会醒过来吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','没问题','score',0),JSON_OBJECT('value','1','label','轻微延迟/偶尔','score',1),JSON_OBJECT('value','2','label','显著延迟/经常','score',2),JSON_OBJECT('value','3','label','延迟严重或没有睡觉','score',3)),NULL,2,1),
(13,'CHOICE','比期望的时间早醒','您会不会比想醒的时间早醒，再也睡不着？',JSON_ARRAY(JSON_OBJECT('value','0','label','没问题','score',0),JSON_OBJECT('value','1','label','轻微提早','score',1),JSON_OBJECT('value','2','label','显著提早','score',2),JSON_OBJECT('value','3','label','严重提早或没有睡觉','score',3)),NULL,3,1),
(13,'CHOICE','总睡眠时间','您一晚上总的睡眠时间够不够？',JSON_ARRAY(JSON_OBJECT('value','0','label','足够','score',0),JSON_OBJECT('value','1','label','轻微不足','score',1),JSON_OBJECT('value','2','label','显著不足','score',2),JSON_OBJECT('value','3','label','严重不足或没有睡觉','score',3)),NULL,4,1),
(13,'CHOICE','总睡眠质量(无论睡多长)','不管睡多久，您觉得总体的睡眠质量怎么样？',JSON_ARRAY(JSON_OBJECT('value','0','label','满意','score',0),JSON_OBJECT('value','1','label','轻微不满','score',1),JSON_OBJECT('value','2','label','显著不满','score',2),JSON_OBJECT('value','3','label','严重不满或没有睡觉','score',3)),NULL,5,1),
(13,'CHOICE','白天情绪','白天您的情绪怎么样？睡眠有没有影响您的心情？',JSON_ARRAY(JSON_OBJECT('value','0','label','正常','score',0),JSON_OBJECT('value','1','label','轻微低落','score',1),JSON_OBJECT('value','2','label','显著低落','score',2),JSON_OBJECT('value','3','label','严重低落','score',3)),NULL,6,1),
(13,'CHOICE','白天身体功能(体力或精神：记忆力、认知力或注意力等)','白天您觉得体力、精神、注意力怎么样？睡眠有没有影响？',JSON_ARRAY(JSON_OBJECT('value','0','label','足够','score',0),JSON_OBJECT('value','1','label','轻微影响','score',1),JSON_OBJECT('value','2','label','显著影响','score',2),JSON_OBJECT('value','3','label','严重影响','score',3)),NULL,7,1),
(13,'CHOICE','白天思睡','白天您会不会犯困、想打瞌睡？',JSON_ARRAY(JSON_OBJECT('value','0','label','无思睡','score',0),JSON_OBJECT('value','1','label','轻微思睡','score',1),JSON_OBJECT('value','2','label','显著思睡','score',2),JSON_OBJECT('value','3','label','严重思睡','score',3)),NULL,8,1);

-- ---------------------------------------------------------------------
-- 表B.14 慢性疼痛数字评定量表 (scale_id=14)，0~10分
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(14,'CHOICE','请用0~10分描述您当前的疼痛程度','请您用0到10分描述现在的疼痛：0分是一点不痛，10分是极度疼痛、生不如死。您给自己打几分？',JSON_ARRAY(JSON_OBJECT('value','0','label','0分 无疼痛','score',0),JSON_OBJECT('value','1','label','1分','score',1),JSON_OBJECT('value','2','label','2分','score',2),JSON_OBJECT('value','3','label','3分','score',3),JSON_OBJECT('value','4','label','4分','score',4),JSON_OBJECT('value','5','label','5分','score',5),JSON_OBJECT('value','6','label','6分','score',6),JSON_OBJECT('value','7','label','7分','score',7),JSON_OBJECT('value','8','label','8分','score',8),JSON_OBJECT('value','9','label','9分','score',9),JSON_OBJECT('value','10','label','10分 极度疼痛','score',10)),NULL,1,1);

-- ---------------------------------------------------------------------
-- 表B.15 慢性疼痛行为评估量表 (scale_id=15)，由评估人员/家人观察，各1~4分
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(15,'CHOICE','面部表情(由评估人员/家人观察)','请家人或照护者观察一下：评估对象面部表情是放松的，还是有些紧绷(皱眉)、完全紧绷(紧闭眼睛)或面部扭曲？',JSON_ARRAY(JSON_OBJECT('value','1','label','表情放松','score',1),JSON_OBJECT('value','2','label','部分紧绷(如皱眉)','score',2),JSON_OBJECT('value','3','label','完全紧绷(如眼睛紧闭)','score',3),JSON_OBJECT('value','4','label','面部扭曲','score',4)),NULL,1,1),
(15,'CHOICE','上肢运动(由评估人员/家人观察)','请家人观察：评估对象上肢是否没有活动、部分弯曲、完全弯曲且手指弯曲，或持续回缩？',JSON_ARRAY(JSON_OBJECT('value','1','label','没有活动','score',1),JSON_OBJECT('value','2','label','部分弯曲','score',2),JSON_OBJECT('value','3','label','完全弯曲且手指弯曲','score',3),JSON_OBJECT('value','4','label','持续回缩','score',4)),NULL,2,1),
(15,'CHOICE','发声(非插管：呻吟、言语抱怨等)','请家人观察：评估对象是否发出与疼痛有关的呻吟、或大声抱怨疼痛？',JSON_ARRAY(JSON_OBJECT('value','1','label','无疼痛相关发声','score',1),JSON_OBJECT('value','2','label','呻吟≤3次/分钟且每次≤3秒','score',2),JSON_OBJECT('value','3','label','呻吟>3次/分钟且每次>3秒','score',3),JSON_OBJECT('value','4','label','咆哮或用"哦/哎哟"等言语抱怨或屏住呼吸','score',4)),NULL,3,1);

-- ---------------------------------------------------------------------
-- 表B.16 压力性损伤风险评估量表 (scale_id=16)，6条目各1~4分，高分低危
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(16,'CHOICE','感觉：对疼痛/压力的感知能力','您对疼痛或压在身上的压力，感觉还灵敏吗？(完全没感觉→非常受限→轻度受限→未受损害)',JSON_ARRAY(JSON_OBJECT('value','1','label','完全受限(对痛/压力无感觉)','score',1),JSON_OBJECT('value','2','label','非常受限(仅对剧烈刺激有反应)','score',2),JSON_OBJECT('value','3','label','轻度受限(对刺痛反应迟钝)','score',3),JSON_OBJECT('value','4','label','未受损害(对痛/压力反应灵敏)','score',4)),NULL,1,1),
(16,'CHOICE','潮湿：皮肤长期浸湿的程度','您身体皮肤经常潮湿吗？(一直湿→非常潮湿→偶尔潮湿→很少潮湿)',JSON_ARRAY(JSON_OBJECT('value','1','label','持久潮湿','score',1),JSON_OBJECT('value','2','label','非常潮湿','score',2),JSON_OBJECT('value','3','label','偶尔潮湿','score',3),JSON_OBJECT('value','4','label','很少潮湿','score',4)),NULL,2,1),
(16,'CHOICE','活动：身体活动的能力','您平时能下床活动吗？',JSON_ARRAY(JSON_OBJECT('value','1','label','卧床不起','score',1),JSON_OBJECT('value','2','label','局限于椅上','score',2),JSON_OBJECT('value','3','label','偶尔步行','score',3),JSON_OBJECT('value','4','label','经常步行','score',4)),NULL,3,1),
(16,'CHOICE','移动：改变/控制体位的能力','您能自己翻身、改变姿势吗？',JSON_ARRAY(JSON_OBJECT('value','1','label','完全不能','score',1),JSON_OBJECT('value','2','label','严重受限','score',2),JSON_OBJECT('value','3','label','轻度受限','score',3),JSON_OBJECT('value','4','label','不受限','score',4)),NULL,4,1),
(16,'CHOICE','营养：日常进食状况','您平时吃饭、营养状况怎么样？',JSON_ARRAY(JSON_OBJECT('value','1','label','非常差','score',1),JSON_OBJECT('value','2','label','可能不足','score',2),JSON_OBJECT('value','3','label','适当','score',3),JSON_OBJECT('value','4','label','良好','score',4)),NULL,5,1),
(16,'CHOICE','摩擦和剪切力','您活动时，皮肤会不会容易与床单摩擦或滑动造成损伤？',JSON_ARRAY(JSON_OBJECT('value','1','label','有问题','score',1),JSON_OBJECT('value','2','label','有潜在问题','score',2),JSON_OBJECT('value','3','label','无明显问题','score',3)),NULL,6,1);

-- ---------------------------------------------------------------------
-- 表B.17 洼田饮水试验 (scale_id=17)，分级1~5
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(17,'CHOICE','洼田饮水试验：请老人坐在稳定椅子上，喝一小杯(30ml)温开水，按平常方式饮水，观察是否呛咳','请在家人看护下，让老人坐在椅子上，喝一小杯温开水，观察是否呛咳、用时多长。请选择最接近的分级：',JSON_ARRAY(JSON_OBJECT('value','1','label','1级：一次喝完，没有呛咳(≤5秒)','score',1),JSON_OBJECT('value','2','label','2级：一次喝完无呛咳但>5秒，或两次以上喝完无呛咳','score',2),JSON_OBJECT('value','3','label','3级：一次喝完，但有呛咳','score',3),JSON_OBJECT('value','4','label','4级：两次或以上喝完，有呛咳','score',4),JSON_OBJECT('value','5','label','5级：频繁呛咳，很难全部喝完','score',5)),NULL,1,1);

-- ---------------------------------------------------------------------
-- 表B.18 2002营养风险筛查量表 (scale_id=18)：初筛(信息项)+终筛
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(18,'CHOICE','初筛1：体质指数(BMI)是否<20.5？','您的体质指数(BMI=体重÷身高²)小于20.5吗？(体重公斤除以身高米的平方)',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',0)),NULL,1,1),
(18,'CHOICE','初筛2：最近3个月有无体重减轻？','最近3个月，您的体重有没有下降？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',0)),NULL,2,1),
(18,'CHOICE','初筛3：最近1周有无膳食摄入量减少？','最近1周，您饭量有没有明显减少、吃得比平时少？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',0)),NULL,3,1),
(18,'CHOICE','初筛4：疾病是否严重(如重症监护、大手术后)？','您现在有没有重病(比如住过重症监护、刚做完大手术)？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',0)),NULL,4,1),
(18,'CHOICE','终筛-营养状态受损程度','您的营养受损情况更接近哪一项？(无/轻度/中度/重度)',JSON_ARRAY(JSON_OBJECT('value','0','label','正常营养状态(0分)','score',0),JSON_OBJECT('value','1','label','轻度：3个月体重丢失>5%，或进食量仅为平时50%~75%','score',1),JSON_OBJECT('value','2','label','中度：2个月体重丢失>5%，或进食量仅为平时25%~50%，或BMI 18.5~20.5','score',2),JSON_OBJECT('value','3','label','重度：1个月体重丢失>5%(或3个月>15%)，或进食量<平时25%，或BMI<18.5','score',3)),NULL,5,1),
(18,'CHOICE','终筛-疾病严重程度','您的疾病严重程度更接近哪一项？(无/轻度/中度/重度)',JSON_ARRAY(JSON_OBJECT('value','0','label','正常能量需求(0分)','score',0),JSON_OBJECT('value','1','label','轻度：如髋骨骨折、肝硬化、慢阻肺、长期血透、糖尿病、肿瘤等','score',1),JSON_OBJECT('value','2','label','中度：如腹部大手术、脑卒中、重症肺炎、血液系统恶性肿瘤等','score',2),JSON_OBJECT('value','3','label','重度：如颅脑损伤、骨髓移植、重症监护(APACHE>10)等','score',3)),NULL,6,1),
(18,'CHOICE','终筛-年龄是否≥70岁？','您今年满70岁了吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否，未满70岁','score',0),JSON_OBJECT('value','1','label','是，已满70岁','score',1)),NULL,7,1);

-- ---------------------------------------------------------------------
-- 表B.19 微营养评定法简表 (scale_id=19)，满分14
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(19,'CHOICE','过去3个月是否因食欲减退、消化不良、咀嚼或吞咽困难而减少食量？','最近3个月，您有没有因为不想吃、消化不良或吃东西费劲，饭量变少了？',JSON_ARRAY(JSON_OBJECT('value','0','label','食量严重减少','score',0),JSON_OBJECT('value','1','label','食量中度减少','score',1),JSON_OBJECT('value','2','label','食量没有改变','score',2)),NULL,1,1),
(19,'CHOICE','过去3个月体重丢失情况','最近3个月，您的体重下降了吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','下降>3公斤','score',0),JSON_OBJECT('value','1','label','不知道','score',1),JSON_OBJECT('value','2','label','下降1~3公斤','score',2),JSON_OBJECT('value','3','label','没有下降','score',3)),NULL,2,1),
(19,'CHOICE','活动能力','您现在活动能力怎么样？',JSON_ARRAY(JSON_OBJECT('value','0','label','长期卧床或坐轮椅','score',0),JSON_OBJECT('value','1','label','可下床/离开轮椅，但不能外出','score',1),JSON_OBJECT('value','2','label','可以外出','score',2)),NULL,3,1),
(19,'CHOICE','过去3个月是否受到心理创伤或有急性疾病？','最近3个月，您有没有受过心理打击，或者生过急病？',JSON_ARRAY(JSON_OBJECT('value','0','label','是','score',0),JSON_OBJECT('value','2','label','否','score',2)),NULL,4,1),
(19,'CHOICE','精神心理问题','您有精神心理方面的问题吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','严重痴呆或抑郁','score',0),JSON_OBJECT('value','1','label','轻度痴呆','score',1),JSON_OBJECT('value','2','label','无精神心理问题','score',2)),NULL,5,1),
(19,'CHOICE','体质指数(BMI)或小腿围','您的体质指数(BMI)或小腿围是多少？(量小腿最粗处周径；或算BMI)',JSON_ARRAY(JSON_OBJECT('value','0','label','BMI<19，或小腿围<31cm','score',0),JSON_OBJECT('value','1','label','BMI 19~21','score',1),JSON_OBJECT('value','2','label','BMI 21~23','score',2),JSON_OBJECT('value','3','label','BMI≥23，或小腿围≥31cm','score',3)),NULL,6,1);

-- ---------------------------------------------------------------------
-- 表B.20 全球领导人营养不良倡议标准及分级 (scale_id=20)，表现型+病因型组合判定
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(20,'CHOICE','表现型-体重减轻：过去6个月内体重减轻>5%，或6个月以上减轻>10%？','您最近体重有明显下降吗？(半年内瘦了超过5%，或更久时间内瘦了超过10%)',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,1,1),
(20,'CHOICE','表现型-低体质指数：<70岁BMI<18.5，或≥70岁BMI<20？','您的体质指数偏低吗？(小于70岁且BMI<18.5，或满70岁且BMI<20)',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,2,1),
(20,'CHOICE','表现型-肌肉减少：根据骨骼肌指数/去脂体质指数判断有无肌肉减少？','医生有没有告诉过您存在肌肉减少(肌少症)？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,3,1),
(20,'CHOICE','病因型-饮食摄入或吸收减少：能量摄入≤50%持续>1周，或持续减少>2周，或有慢性消化道症状？','您最近吃得很少(不足平时一半且持续一周以上)，或长时间吃得少，或有慢性消化不良症状吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,4,1),
(20,'CHOICE','病因型-炎症状态：有急性疾病/损伤，或慢性疾病相关炎症？','您现在有急性病、受伤，或者慢性炎症疾病(如慢阻肺、类风湿等)吗？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,5,1);

-- ---------------------------------------------------------------------
-- 表B.21 谵妄(意识模糊)评估表 (scale_id=21)，条目组合判定
-- ---------------------------------------------------------------------
INSERT INTO `scale_question` (`scale_id`,`question_type`,`question_text`,`dialog_text`,`options`,`jump_rule`,`sort_order`,`enabled`) VALUES
(21,'CHOICE','条目1-急性起病与病情波动：是否存在精神状态的急性变化，或一天内行为时轻时重/时有时无？','请您(或家人)回忆：最近是否突然出现精神状态的急性变化(比如突然犯糊涂)，或者一天里一会儿清楚一会儿糊涂、时轻时重？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,1,1),
(21,'CHOICE','条目2-注意力不集中：注意力是否难以集中，容易分散或跟不上话题？','注意力方面，您(或家人)是否觉得注意力很难集中、容易走神、跟不上别人说话？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,2,1),
(21,'CHOICE','条目3-思维混乱：思维是否混乱或不连贯，说话跑题、无逻辑、突然换话题？','思维方面，是否觉得说话混乱、不连贯，容易跑题、没逻辑，或突然从一个话题跳到另一个？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,3,1),
(21,'CHOICE','条目4-意识水平改变：当前意识是否异常(过度警觉/嗜睡/昏睡)？','意识方面，是否有异常？比如过度警觉、容易受惊，或特别嗜睡、叫不醒？',JSON_ARRAY(JSON_OBJECT('value','0','label','否','score',0),JSON_OBJECT('value','1','label','是','score',1)),NULL,4,1);

-- ---------------------------------------------------------------------
-- 语义映射词典（口语化表述 → 标准选项，全局通用）
-- ---------------------------------------------------------------------
INSERT INTO `semantic_dictionary` (`question_id`,`option_value`,`synonyms`,`match_type`,`sort_order`) VALUES
(NULL,'1','是,对的,有,会,能,经常,总是,是的,有的,行,可以,困难,吃力,累','CONTAINS',1),
(NULL,'0','否,没有,不会,不能,不,从不,无,不是,不行','CONTAINS',1);
