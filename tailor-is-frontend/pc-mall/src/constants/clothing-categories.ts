/**
 * Tailor IS 裁智云 - 服装分类体系常量
 * 版本：v1.0
 * 结构：一级（性别/适用人群） → 二级（品类大类） → 三级（具体款式）
 * 作用：前端分类展示、侧边栏筛选、导航下拉、搜索联想、数据统计全项目统一使用
 * 维护机制：本文件为唯一分类常量源，新增分类在此修改，禁止在组件内硬编码
 */

import type { ClothingCategory, DesignElement } from '../types';

/** =======================================================
 * 一级分类编码（性别/适用人群）
 * ======================================================= */
export const CLOTHING_ROOT_WOMEN = 'women';
export const CLOTHING_ROOT_MEN = 'men';
export const CLOTHING_ROOT_CHILDREN = 'children';

/** =======================================================
 * 设计元素常量（色彩、廓形、分割线、比例、工艺细节）
 * ======================================================= */
export const DESIGN_ELEMENTS: DesignElement = {
    color: [
        { id: 'spring', name: '春夏色系', desc: '浅粉、薄荷、柠檬、天空蓝', swatch: ['#FFB5C5', '#9AFF9A', '#FFF68F', '#87CEEB'] },
        { id: 'autumn', name: '秋冬色系', desc: '驼色、墨绿、酒红、深灰', swatch: ['#C19A6B', '#2F4F4F', '#722F37', '#4A4A4A'] },
        { id: 'neutral', name: '基础色系', desc: '黑白灰米，百搭永不过时', swatch: ['#000000', '#FFFFFF', '#808080', '#F5F5DC'] },
        { id: 'bright', name: '高饱和色系', desc: '正红、亮黄、宝蓝、鲜橙', swatch: ['#E63946', '#FFD700', '#1D3557', '#FF6B35'] },
        { id: 'pastel', name: '莫兰迪色系', desc: '低饱和高级灰调', swatch: ['#B4A7D6', '#EAD1DC', '#D5E8D4', '#F8CECC'] },
    ],
    silhouette: [
        { id: 'H', name: 'H型（直筒）', desc: '肩腰臀同宽，利落挺括，职场首选' },
        { id: 'X', name: 'X型（收腰）', desc: '强调腰线，优雅性感，连衣裙经典' },
        { id: 'A', name: 'A型（上窄下宽）', desc: '下身蓬松，遮胯显瘦，少女风' },
        { id: 'O', name: 'O型（茧型）', desc: '中部饱满，慵懒随性，大衣/卫衣' },
        { id: 'V', name: 'V型（上宽下窄）', desc: '肩部突出，气场强大，西装/风衣' },
        { id: 'S', name: 'S型（紧身包臀）', desc: '曲线毕露，性感正式，晚礼服' },
    ],
    partitionLine: [
        { id: 'princess', name: '公主线分割', desc: '纵向从肩至腰缝，修身显瘦' },
        { id: 'waist', name: '腰节线分割', desc: '横向收腰，比例分明' },
        { id: 'yoke', name: '育克分割', desc: '肩胸/臀部分段拼接，层次丰富' },
        { id: 'raglan', name: '插肩线', desc: '斜向肩缝，休闲运动' },
        { id: 'setIn', name: '装袖线', desc: '圆袖拼接，正装经典' },
    ],
    proportion: [
        { id: '37', name: '三七比例', desc: '高腰设计，腿长既视感最强' },
        { id: '55', name: '五五比例', desc: '自然均衡，休闲舒适' },
        { id: '28', name: '二八比例', desc: '超高腰+短上衣，超模比例' },
        { id: 'longSleeve', name: '九分袖长', desc: '露出手腕最细处，显利落' },
        { id: 'cropped', name: '短款露腰', desc: '衣长至腰线，元气减龄' },
    ],
    detail: [
        { id: 'pleat', name: '褶裥工艺', desc: '压褶/抽褶/风琴褶，灵动量感' },
        { id: 'embroidery', name: '刺绣装饰', desc: '手推绣/电脑绣，国风高级感' },
        { id: 'printing', name: '印花图案', desc: '数码印/热转移，个性表达' },
        { id: 'button', name: '扣饰设计', desc: '牛角扣/珍珠扣/金属扣，细节点睛' },
        { id: 'pocket', name: '口袋设计', desc: '贴袋/插袋/立体袋，实用兼具造型' },
        { id: 'zipper', name: '拉链设计', desc: '隐形拉链/金属拉链，开合便捷' },
        { id: 'binding', name: '包边工艺', desc: '撞色滚边/本料包边，精致收尾' },
        { id: 'lining', name: '里料工艺', desc: '全里/半里/包边缝，穿着舒适无静电' },
    ],
};

/** =======================================================
 * 童装年龄区分
 * ======================================================= */
export const CHILDREN_AGE_GROUPS = [
    { id: 'baby', name: '婴童（0-1岁）', height: '59-80cm' },
    { id: 'toddler', name: '小童（1-4岁）', height: '80-110cm' },
    { id: 'child', name: '大童（4-12岁）', height: '110-160cm' },
    { id: 'teen', name: '青少年（12-16岁）', height: '150-175cm' },
];

/** =======================================================
 * 三级分类树（唯一数据源，所有页面统一引用）
 * 结构保证：无交叉重叠、层级关系清晰、编码稳定
 * ======================================================= */
export const CLOTHING_CATEGORY_TREE: ClothingCategory[] = [
    // ---------- 女装 ----------
    {
        id: 'women',
        code: CLOTHING_ROOT_WOMEN,
        name: '女装系列',
        level: 1,
        parentId: 0,
        icon: '👗',
        sort: 1,
        desc: '18岁以上成年女性全品类服装',
        children: [
            {
                id: 'women-top', name: '上装', level: 2, parentId: 'women', code: 'women-top',
                icon: '👚', sort: 1, desc: '衬衫、西装、风衣、夹克、针织衫、卫衣、毛衣',
                children: [
                    { id: 'women-top-shirt', name: '衬衫', level: 3, parentId: 'women-top', code: 'women-top-shirt', sort: 1, desc: '通勤修身、宽松休闲、法式优雅、立领蝴蝶结' },
                    { id: 'women-top-suit', name: '西装', level: 3, parentId: 'women-top', code: 'women-top-suit', sort: 2, desc: '职场正装、休闲廓形、短款小西装、oversize宽肩' },
                    { id: 'women-top-coat', name: '风衣', level: 3, parentId: 'women-top', code: 'women-top-coat', sort: 3, desc: '经典双排扣、英伦战壕、系带浴袍、短款风衣' },
                    { id: 'women-top-jacket', name: '夹克', level: 3, parentId: 'women-top', code: 'women-top-jacket', sort: 4, desc: '飞行员夹克、牛仔夹克、PU皮夹克、棒球服' },
                    { id: 'women-top-knit', name: '针织衫', level: 3, parentId: 'women-top', code: 'women-top-knit', sort: 5, desc: '坑条打底、镂空开衫、V领羊毛、泡泡袖针织' },
                    { id: 'women-top-hoodie', name: '卫衣', level: 3, parentId: 'women-top', code: 'women-top-hoodie', sort: 6, desc: '连帽套头、圆领宽松、拉链开衫、加绒加厚' },
                    { id: 'women-top-sweater', name: '毛衣', level: 3, parentId: 'women-top', code: 'women-top-sweater', sort: 7, desc: '高领羊毛、圆领羊绒、麻花针织、卡通提花' },
                ],
            },
            {
                id: 'women-bottom', name: '下装', level: 2, parentId: 'women', code: 'women-bottom',
                icon: '👖', sort: 2, desc: '西裤、牛仔裤、阔腿裤、半身裙、打底裤、短裤',
                children: [
                    { id: 'women-bottom-suitPant', name: '西裤', level: 3, parentId: 'women-bottom', code: 'women-bottom-suitPant', sort: 1, desc: '高腰直筒、九分烟管、拖地阔腿、小脚通勤' },
                    { id: 'women-bottom-jeans', name: '牛仔裤', level: 3, parentId: 'women-bottom', code: 'women-bottom-jeans', sort: 2, desc: '高腰直筒、修身小脚、阔腿拖地、毛边短裤' },
                    { id: 'women-bottom-widePant', name: '阔腿裤', level: 3, parentId: 'women-bottom', code: 'women-bottom-widePant', sort: 3, desc: '垂坠西装料、牛仔休闲、针织弹力、真丝轻薄' },
                    { id: 'women-bottom-skirt', name: '半身裙', level: 3, parentId: 'women-bottom', code: 'women-bottom-skirt', sort: 4, desc: 'A字伞裙、直筒包臀、百褶长裙、不规则半裙' },
                    { id: 'women-bottom-legging', name: '打底裤', level: 3, parentId: 'women-bottom', code: 'women-bottom-legging', sort: 5, desc: '保暖加绒、鲨鱼裤塑形、鲨鱼皮压力、纯棉无痕' },
                    { id: 'women-bottom-shortPant', name: '短裤', level: 3, parentId: 'women-bottom', code: 'women-bottom-shortPant', sort: 6, desc: '高腰A字、西装百慕大、牛仔热裤、运动休闲' },
                ],
            },
            {
                id: 'women-set', name: '连体/套装', level: 2, parentId: 'women', code: 'women-set',
                icon: '💃', sort: 3, desc: '连衣裙、两件套、国风套装、通勤套装',
                children: [
                    { id: 'women-set-dress', name: '连衣裙', level: 3, parentId: 'women-set', code: 'women-set-dress', sort: 1, desc: '衬衫裙、茶歇裙、吊带裙、法式裹身、针织连衣裙' },
                    { id: 'women-set-twopiece', name: '两件套', level: 3, parentId: 'women-set', code: 'women-set-twopiece', sort: 2, desc: '上下同料同色、通勤干练、度假休闲、小香风套装' },
                    { id: 'women-set-hanfu', name: '国风套装', level: 3, parentId: 'women-set', code: 'women-set-hanfu', sort: 3, desc: '改良旗袍、新中式盘扣、汉服日常、唐装对襟' },
                    { id: 'women-set-commute', name: '通勤套装', level: 3, parentId: 'women-set', code: 'women-set-commute', sort: 4, desc: '西装三件套、西装半裙、衬衫马甲+西裤' },
                ],
            },
            {
                id: 'women-special', name: '特殊品类', level: 2, parentId: 'women', code: 'women-special',
                icon: '🌟', sort: 4, desc: '羽绒服、棉服、晚礼服、吊带内搭',
                children: [
                    { id: 'women-special-down', name: '羽绒服', level: 3, parentId: 'women-special', code: 'women-special-down', sort: 1, desc: '短款面包、长款过膝、极寒加厚、轻薄羽绒' },
                    { id: 'women-special-cotton', name: '棉服', level: 3, parentId: 'women-special', code: 'women-special-cotton', sort: 2, desc: '棉衣、派克、绗缝棉服、加绒棉服' },
                    { id: 'women-special-gown', name: '晚礼服', level: 3, parentId: 'women-special', code: 'women-special-gown', sort: 3, desc: '长款拖地、鱼尾修身、短款小礼服、伴娘裙' },
                    { id: 'women-special-camisole', name: '吊带内搭', level: 3, parentId: 'women-special', code: 'women-special-camisole', sort: 4, desc: '真丝吊带、蕾丝打底、美背背心、纯棉内搭' },
                ],
            },
        ],
    },
    // ---------- 男装 ----------
    {
        id: 'men',
        code: CLOTHING_ROOT_MEN,
        name: '男装系列',
        level: 1,
        parentId: 0,
        icon: '👔',
        sort: 2,
        desc: '18岁以上成年男性全品类服装',
        children: [
            {
                id: 'men-top', name: '上装', level: 2, parentId: 'men', code: 'men-top',
                icon: '🎽', sort: 1, desc: '正装衬衫、商务西装、工装夹克、Polo、大衣',
                children: [
                    { id: 'men-top-shirt', name: '正装衬衫', level: 3, parentId: 'men-top', code: 'men-top-shirt', sort: 1, desc: '纯棉免烫、弹力修身、商务白衬衫、温莎领法式' },
                    { id: 'men-top-suit', name: '商务西装', level: 3, parentId: 'men-top', code: 'men-top-suit', sort: 2, desc: '单排扣平驳领、双排扣戗驳领、三件套马甲、休闲单西' },
                    { id: 'men-top-jacket', name: '工装夹克', level: 3, parentId: 'men-top', code: 'men-top-jacket', sort: 3, desc: '牛仔工装、猎装夹克、M65野战、复古工装' },
                    { id: 'men-top-polo', name: 'Polo衫', level: 3, parentId: 'men-top', code: 'men-top-polo', sort: 4, desc: '珠地网眼、商务翻领、短袖夏款、长袖春秋' },
                    { id: 'men-top-coat', name: '大衣', level: 3, parentId: 'men-top', code: 'men-top-coat', sort: 5, desc: '双面呢大衣、驼色风衣、羊毛呢子、军旅大衣' },
                ],
            },
            {
                id: 'men-bottom', name: '下装', level: 2, parentId: 'men', code: 'men-bottom',
                icon: '👖', sort: 2, desc: '西裤、工装裤、休闲裤、牛仔裤、运动裤',
                children: [
                    { id: 'men-bottom-suitPant', name: '西裤', level: 3, parentId: 'men-bottom', code: 'men-bottom-suitPant', sort: 1, desc: '修身直筒、九分裤脚、免烫垂坠、商务正装' },
                    { id: 'men-bottom-cargo', name: '工装裤', level: 3, parentId: 'men-bottom', code: 'men-bottom-cargo', sort: 2, desc: '多口袋战术、复古水洗、束脚工装、宽松直筒' },
                    { id: 'men-bottom-casual', name: '休闲裤', level: 3, parentId: 'men-bottom', code: 'men-bottom-casual', sort: 3, desc: '纯棉卡其、弹力休闲、亚麻轻薄、灯芯绒秋冬' },
                    { id: 'men-bottom-jeans', name: '牛仔裤', level: 3, parentId: 'men-bottom', code: 'men-bottom-jeans', sort: 4, desc: '原色养牛、水洗做旧、直筒修身、锥形小脚' },
                    { id: 'men-bottom-sport', name: '运动裤', level: 3, parentId: 'men-bottom', code: 'men-bottom-sport', sort: 5, desc: '针织卫裤、束脚收口、速干透气、加绒保暖' },
                ],
            },
            {
                id: 'men-set', name: '套装', level: 2, parentId: 'men', code: 'men-set',
                icon: '🧥', sort: 3, desc: '商务正装套、工装套、运动套',
                children: [
                    { id: 'men-set-business', name: '商务正装套', level: 3, parentId: 'men-set', code: 'men-set-business', sort: 1, desc: '西装+西裤+衬衫+领带、双排扣礼服、修身三件套' },
                    { id: 'men-set-cargo', name: '工装套装', level: 3, parentId: 'men-set', code: 'men-set-cargo', sort: 2, desc: '牛仔工装套装、复古水洗工装、户外冲锋套装' },
                    { id: 'men-set-sport', name: '运动套装', level: 3, parentId: 'men-set', code: 'men-set-sport', sort: 3, desc: '卫衣卫裤两件套、速干训练装、加绒秋冬运动服' },
                ],
            },
            {
                id: 'men-special', name: '特殊品类', level: 2, parentId: 'men', code: 'men-special',
                icon: '🌟', sort: 4, desc: '羽绒服、冲锋衣、马甲',
                children: [
                    { id: 'men-special-down', name: '羽绒服', level: 3, parentId: 'men-special', code: 'men-special-down', sort: 1, desc: '短款立领、长款过膝、商务轻薄、户外极寒' },
                    { id: 'men-special-hardshell', name: '冲锋衣', level: 3, parentId: 'men-special', code: 'men-special-hardshell', sort: 2, desc: '三层压胶防水、软壳抓绒、三合一可拆卸、单冲春秋' },
                    { id: 'men-special-vest', name: '马甲', level: 3, parentId: 'men-special', code: 'men-special-vest', sort: 3, desc: '羽绒马甲、西装马甲、战术马甲、钓鱼摄影背心' },
                ],
            },
        ],
    },
    // ---------- 童装 ----------
    {
        id: 'children',
        code: CLOTHING_ROOT_CHILDREN,
        name: '童装系列',
        level: 1,
        parentId: 0,
        icon: '🧸',
        sort: 3,
        desc: '0-16岁婴童/小童/大童全品类服装',
        children: [
            {
                id: 'children-baby', name: '婴童（0-1岁）', level: 2, parentId: 'children', code: 'children-baby',
                icon: '👶', sort: 1, ageGroup: 'baby', desc: '哈衣、爬服、包屁衣、分体套装',
                children: [
                    { id: 'children-baby-romper', name: '哈衣（连体衣）', level: 3, parentId: 'children-baby', code: 'children-baby-romper', sort: 1, desc: '前开扣、信封领、侧开襟、纯棉A类' },
                    { id: 'children-baby-creeper', name: '爬服', level: 3, parentId: 'children-baby', code: 'children-baby-creeper', sort: 2, desc: '护膝爬服、地板袜套装、包脚爬服' },
                    { id: 'children-baby-bodysuit', name: '包屁衣', level: 3, parentId: 'children-baby', code: 'children-baby-bodysuit', sort: 3, desc: '三角包屁、短袖夏款、长袖保暖、纯棉包裆' },
                    { id: 'children-baby-set', name: '分体套装', level: 3, parentId: 'children-baby', code: 'children-baby-set', sort: 4, desc: '上衣+长裤两件套、和尚服系带、蝴蝶衣套装' },
                ],
            },
            {
                id: 'children-kids', name: '中大童（1-12岁）', level: 2, parentId: 'children', code: 'children-kids',
                icon: '🎒', sort: 2, ageGroup: 'child', desc: '衬衫、卫衣、连衣裙、校服、外套、裤装',
                children: [
                    { id: 'children-kids-shirt', name: '衬衫', level: 3, parentId: 'children-kids', code: 'children-kids-shirt', sort: 1, desc: '纯棉免烫、格子牛津、女童蕾丝领、男童正装白' },
                    { id: 'children-kids-hoodie', name: '卫衣', level: 3, parentId: 'children-kids', code: 'children-kids-hoodie', sort: 2, desc: '连帽卡通、圆领套头、加绒加厚、亲子同款' },
                    { id: 'children-kids-dress', name: '连衣裙', level: 3, parentId: 'children-kids', code: 'children-kids-dress', sort: 3, desc: '公主蓬蓬裙、纯棉碎花、学院百褶、运动网球裙' },
                    { id: 'children-kids-uniform', name: '校服', level: 3, parentId: 'children-kids', code: 'children-kids-uniform', sort: 4, desc: '英伦学院风、运动套装、衬衫马甲、西装正装款' },
                    { id: 'children-kids-coat', name: '外套', level: 3, parentId: 'children-kids', code: 'children-kids-coat', sort: 5, desc: '棒球服、牛仔外套、风衣大衣、加绒冲锋衣' },
                    { id: 'children-kids-pants', name: '裤装', level: 3, parentId: 'children-kids', code: 'children-kids-pants', sort: 6, desc: '牛仔裤、休闲裤、运动裤、打底裤、加绒棉裤' },
                ],
            },
            {
                id: 'children-special', name: '特殊品类', level: 2, parentId: 'children', code: 'children-special',
                icon: '🌟', sort: 3, desc: '儿童羽绒服、防晒衣、演出礼服',
                children: [
                    { id: 'children-special-down', name: '儿童羽绒服', level: 3, parentId: 'children-special', code: 'children-special-down', sort: 1, desc: '短款轻薄、中长款加厚、连体羽绒服、羽绒裤套装' },
                    { id: 'children-special-sun', name: '防晒衣', level: 3, parentId: 'children-special', code: 'children-special-sun', sort: 2, desc: 'UPF50+冰丝凉感、带帽披肩款、亲子防晒、透气网眼' },
                    { id: 'children-special-dress', name: '演出礼服', level: 3, parentId: 'children-special', code: 'children-special-dress', sort: 3, desc: '女童公主蓬蓬裙、男童西装花童、节日演出服、主持人礼服' },
                ],
            },
        ],
    },
];

/** =======================================================
 * 非服装类目（设计服务 / 面料辅料 / 设备工具）
 * 与服装分类体系互补，用于首页分类导航的全品类展示
 * 跳转规则：服装类目 → /patterns?category=xxx；非服装类目 → /products?keyword=xxx
 * ======================================================= */
export const EXTRA_CATEGORY_TREE: ClothingCategory[] = [
    {
        id: 'design-service', code: 'design-service', name: '设计服务', level: 1, parentId: 0,
        icon: '🎨', sort: 4, desc: '3D试穿、AI设计、版权检测、灵感库',
        children: [
            { id: 'design-service-3d', name: '3D试穿预览', level: 2, parentId: 'design-service', code: 'design-service-3d', sort: 1, desc: '虚拟模特试穿、版型调整可视化' },
            { id: 'design-service-ai', name: 'AI智能设计', level: 2, parentId: 'design-service', code: 'design-service-ai', sort: 2, desc: 'AI生成款式图、配色推荐、面料匹配' },
            { id: 'design-service-copyright', name: '版权存证检测', level: 2, parentId: 'design-service', code: 'design-service-copyright', sort: 3, desc: '区块链版权存证、AI相似度侵权检测' },
            { id: 'design-service-inspiration', name: '灵感库协作', level: 2, parentId: 'design-service', code: 'design-service-inspiration', sort: 4, desc: '设计素材库、团队协作、版本管理' },
        ],
    },
    {
        id: 'fabric', code: 'fabric', name: '面料辅料', level: 1, parentId: 0,
        icon: '🧵', sort: 5, desc: '棉布、丝绸、化纤、拉链、纽扣',
        children: [
            { id: 'fabric-cotton', name: '棉布面料', level: 2, parentId: 'fabric', code: 'fabric-cotton', sort: 1, desc: '纯棉、帆布、府绸、灯芯绒' },
            { id: 'fabric-silk', name: '丝绸面料', level: 2, parentId: 'fabric', code: 'fabric-silk', sort: 2, desc: '真丝、雪纺、缎面、欧根纱' },
            { id: 'fabric-chemical', name: '化纤面料', level: 2, parentId: 'fabric', code: 'fabric-chemical', sort: 3, desc: '涤纶、锦纶、氨纶、混纺' },
            { id: 'fabric-linen', name: '麻布面料', level: 2, parentId: 'fabric', code: 'fabric-linen', sort: 4, desc: '亚麻、棉麻、苎麻、混纺麻' },
            { id: 'fabric-zipper', name: '拉链配件', level: 2, parentId: 'fabric', code: 'fabric-zipper', sort: 5, desc: '金属拉链、隐形拉链、树脂拉链' },
            { id: 'fabric-button', name: '纽扣配件', level: 2, parentId: 'fabric', code: 'fabric-button', sort: 6, desc: '牛角扣、珍珠扣、金属扣、树脂扣' },
        ],
    },
    {
        id: 'equipment', code: 'equipment', name: '设备工具', level: 1, parentId: 0,
        icon: '🔧', sort: 6, desc: '缝纫机、绣花机、熨烫、裁剪',
        children: [
            { id: 'equipment-sewing', name: '缝纫机', level: 2, parentId: 'equipment', code: 'equipment-sewing', sort: 1, desc: '家用缝纫机、工业平车、包缝机' },
            { id: 'equipment-embroidery', name: '绣花机', level: 2, parentId: 'equipment', code: 'equipment-embroidery', sort: 2, desc: '电脑绣花机、多头绣花、家用绣花' },
            { id: 'equipment-ironing', name: '熨烫设备', level: 2, parentId: 'equipment', code: 'equipment-ironing', sort: 3, desc: '蒸汽熨斗、烫台、锅炉熨烫' },
            { id: 'equipment-cutting', name: '裁剪工具', level: 2, parentId: 'equipment', code: 'equipment-cutting', sort: 4, desc: '裁布剪、轮刀、裁床、切割机' },
        ],
    },
];

/** =======================================================
 * 合并后的全品类分类树（服装 + 非服装，用于首页分类导航等全品类展示场景）
 * ======================================================= */
export const ALL_CATEGORY_TREE: ClothingCategory[] = [
    ...CLOTHING_CATEGORY_TREE,
    ...EXTRA_CATEGORY_TREE,
];

/** =======================================================
 * 工具：扁平化分类树（方便搜索、筛选、统计）
 * ======================================================= */
export function flattenCategories(tree: ClothingCategory[] = CLOTHING_CATEGORY_TREE): ClothingCategory[] {
    const result: ClothingCategory[] = [];
    function walk(list: ClothingCategory[]) {
        for (const item of list) {
            result.push(item);
            if (item.children && item.children.length > 0) {
                walk(item.children);
            }
        }
    }
    walk(tree);
    return result;
}

/** =======================================================
 * 工具：根据 id 查找分类节点（含完整路径）
 * ======================================================= */
export function findCategoryPath(id: string, tree: ClothingCategory[] = CLOTHING_CATEGORY_TREE): ClothingCategory[] {
    const path: ClothingCategory[] = [];
    function walk(list: ClothingCategory[]): boolean {
        for (const item of list) {
            path.push(item);
            if (item.id === id) return true;
            if (item.children && walk(item.children)) return true;
            path.pop();
        }
        return false;
    }
    walk(tree);
    return path;
}

/** =======================================================
 * 工具：按 id 查找单个分类
 * ======================================================= */
export function findCategory(id: string, tree: ClothingCategory[] = CLOTHING_CATEGORY_TREE): ClothingCategory | null {
    for (const item of tree) {
        if (item.id === id) return item;
        if (item.children) {
            const found = findCategory(id, item.children);
            if (found) return found;
        }
    }
    return null;
}
