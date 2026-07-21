#!/usr/bin/env python3
"""Build the supermarket seed dataset and download traceable real photos from Wikimedia Commons."""

from __future__ import annotations

import html
import hashlib
import json
import re
import time
import urllib.parse
import urllib.request
import urllib.error
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/seed/supermarket-v1"
IMAGE_DIR = OUTPUT / "images"
API = "https://commons.wikimedia.org/w/api.php"
USER_AGENT = "AI-Commerce-Dataset-Builder/1.0 (local development dataset)"

CATEGORIES = [
    ("fresh", None, "生鲜果蔬", 1, 10), ("fresh-fruit", "fresh", "新鲜水果", 2, 10),
    ("fresh-vegetable", "fresh", "时令蔬菜", 2, 20),
    ("meat-dairy", None, "肉禽蛋奶", 1, 20), ("meat", "meat-dairy", "肉禽水产", 2, 10),
    ("dairy-egg", "meat-dairy", "蛋品乳品", 2, 20),
    ("grain-oil", None, "粮油调味", 1, 30), ("grain", "grain-oil", "米面杂粮", 2, 10),
    ("oil-seasoning", "grain-oil", "食用油调味", 2, 20),
    ("snack", None, "休闲零食", 1, 40), ("biscuit-puff", "snack", "饼干膨化", 2, 10),
    ("nut-dried", "snack", "坚果果干", 2, 20),
    ("beverage", None, "酒水饮料", 1, 50), ("water-juice", "beverage", "饮用水饮料", 2, 10),
    ("coffee-tea", "beverage", "咖啡茶饮", 2, 20),
    ("cleaning", None, "家庭清洁", 1, 60), ("laundry-clean", "cleaning", "衣物家居清洁", 2, 10),
    ("paper-disposable", "cleaning", "纸品一次性", 2, 20),
    ("personal", None, "个护美妆", 1, 70), ("hair-body", "personal", "洗发沐浴", 2, 10),
    ("oral-hand", "personal", "口腔手部护理", 2, 20),
    ("home-kitchen", None, "家居厨具", 1, 80), ("cookware", "home-kitchen", "锅具水杯", 2, 10),
    ("storage", "home-kitchen", "收纳整理", 2, 20),
    ("baby-pet", None, "母婴宠物", 1, 90), ("baby", "baby-pet", "婴童用品", 2, 10),
    ("pet", "baby-pet", "宠物食品", 2, 20),
    ("electronics", None, "家电数码", 1, 100), ("small-appliance", "electronics", "生活小家电", 2, 10),
    ("digital-accessory", "electronics", "数码配件", 2, 20),
]

BRANDS = [
    ("freshjoy", "鲜享家", "产地直采生鲜品牌"), ("pasture", "原野牧场", "蛋奶与肉禽品牌"),
    ("grainfield", "谷禾良品", "米面粮油品牌"), ("kitchen", "百味厨房", "家庭调味品牌"),
    ("snacklab", "食光小铺", "休闲零食品牌"), ("nuts", "每日坚果社", "坚果果干品牌"),
    ("clear", "清泉里", "饮用水与果汁品牌"), ("roast", "慢焙时光", "咖啡茶饮品牌"),
    ("cleanhome", "净家", "家庭清洁品牌"), ("softpaper", "柔纸坊", "生活用纸品牌"),
    ("care", "本真护理", "个人护理品牌"), ("home", "简居", "家居厨具品牌"),
    ("babycare", "小芽成长", "婴童用品品牌"), ("petjoy", "毛球伙伴", "宠物食品品牌"),
    ("smartlife", "智生活", "生活家电与数码配件品牌"),
]

PRODUCTS = [
    ("SM-FRU-001", "红富士苹果 6枚装", "脆甜多汁，家庭分享装", "fresh-fruit", "freshjoy", 29.90, 35.90, 180, "red apples close up fruit", "苹果,红富士,新鲜水果", "早餐,下午茶,家庭囤货", "家庭,上班族", {"产地":"山东","净含量":"约1.2kg","储存":"冷藏"}),
    ("SM-FRU-002", "进口香蕉 1kg", "自然成熟，软糯香甜", "fresh-fruit", "freshjoy", 15.80, 19.90, 220, "bunch bananas fruit", "香蕉,进口水果,即食", "早餐,健身补给", "儿童,健身人群", {"产地":"菲律宾","净含量":"1kg","成熟度":"即食"}),
    ("SM-VEG-001", "番茄 500g", "酸甜饱满，炒菜生食皆宜", "fresh-vegetable", "freshjoy", 8.90, 11.90, 160, "fresh tomatoes vegetable", "番茄,西红柿,蔬菜", "家庭烹饪,沙拉", "家庭", {"产地":"云南","净含量":"500g","等级":"一级"}),
    ("SM-EGG-001", "谷物鲜鸡蛋 20枚", "蛋黄饱满，日期新鲜", "dairy-egg", "pasture", 26.90, 32.90, 130, "chicken eggs carton", "鸡蛋,谷物蛋,蛋品", "早餐,烘焙,家庭烹饪", "家庭", {"枚数":"20枚","保质期":"30天","储存":"冷藏"}),
    ("SM-DAI-001", "纯牛奶 250ml×12盒", "原生乳蛋白，常温整箱", "dairy-egg", "pasture", 49.90, 59.90, 96, "milk glass bottle dairy", "牛奶,纯牛奶,整箱", "早餐,学生营养", "学生,家庭", {"规格":"250ml×12","蛋白质":"3.2g/100ml","储存":"常温"}),
    ("SM-MEA-001", "冷鲜鸡胸肉 500g", "低脂高蛋白，独立锁鲜", "meat", "pasture", 19.90, 25.90, 85, "raw chicken breast meat", "鸡胸肉,冷鲜,高蛋白", "健身餐,家庭烹饪", "健身人群,家庭", {"净含量":"500g","加工":"去皮","储存":"0-4℃"}),
    ("SM-GRA-001", "东北长粒香米 5kg", "当季新米，米香自然", "grain", "grainfield", 42.90, 52.90, 150, "white rice grains bowl", "大米,东北米,长粒香", "家庭主食,囤货", "家庭", {"产地":"黑龙江","净含量":"5kg","等级":"一级"}),
    ("SM-GRA-002", "高筋小麦粉 2.5kg", "适合面包面条，筋度稳定", "grain", "grainfield", 21.90, 26.90, 115, "wheat flour bowl", "面粉,高筋粉,小麦粉", "烘焙,面食", "家庭,烘焙爱好者", {"净含量":"2.5kg","类型":"高筋","保质期":"12个月"}),
    ("SM-OIL-001", "压榨花生油 700ml", "物理压榨，浓郁花生香", "oil-seasoning", "kitchen", 32.90, 39.90, 88, "peanut oil bottle cooking", "花生油,食用油,压榨", "中式烹饪,家庭囤货", "家庭", {"净含量":"700ml","工艺":"物理压榨","等级":"一级"}),
    ("SM-SNA-001", "黄油曲奇饼干 400g", "酥松奶香，独立小包装", "biscuit-puff", "snacklab", 22.90, 29.90, 190, "butter cookies biscuits", "曲奇,饼干,黄油", "下午茶,办公室分享", "家庭,上班族", {"净含量":"400g","包装":"独立装","口味":"黄油"}),
    ("SM-SNA-002", "原味薄脆饼 120g", "薄脆咸香，分享大包装", "biscuit-puff", "snacklab", 9.90, 12.90, 260, "potato chips bowl", "薄脆饼,饼干,原味", "追剧,聚会", "年轻人", {"净含量":"120g","口味":"原味","储存":"阴凉干燥"}),
    ("SM-NUT-001", "混合原味坚果 250g", "多种坚果搭配，轻烘焙无额外加盐", "nut-dried", "nuts", 29.90, 36.90, 135, "almonds nuts bowl", "混合坚果,巴旦木,轻烘焙", "办公室零食,能量补给", "上班族,健身人群", {"净含量":"250g","配料":"巴旦木、核桃、花生等","工艺":"轻烘焙"}),
    ("SM-DRI-001", "天然矿泉水 330ml×12", "清爽甘冽，便携整箱", "water-juice", "clear", 23.90, 29.90, 300, "mineral water bottles", "矿泉水,饮用水,整箱", "家庭囤货,户外", "全人群", {"规格":"330ml×12","水源":"天然水源","包装":"玻璃瓶"}),
    ("SM-DRI-002", "100%橙汁 1L", "非浓缩还原，果香清新", "water-juice", "clear", 18.90, 22.90, 105, "orange juice glass oranges", "橙汁,果汁,NFC", "早餐,聚会", "家庭", {"净含量":"1L","果汁含量":"100%","储存":"冷藏"}),
    ("SM-COF-001", "中度烘焙咖啡豆 500g", "坚果焦糖风味，适合手冲", "coffee-tea", "roast", 68.00, 79.00, 72, "roasted coffee beans", "咖啡豆,中度烘焙,手冲", "居家咖啡,办公室", "咖啡爱好者", {"净含量":"500g","烘焙度":"中度","产区":"云南"}),
    ("SM-CLE-001", "浓缩洗衣液 3L", "低泡易漂，清新去渍", "laundry-clean", "cleanhome", 45.90, 55.90, 140, "laundry detergent bottle", "洗衣液,浓缩,去渍", "机洗,手洗", "家庭", {"净含量":"3L","香型":"清新","适用":"机洗/手洗"}),
    ("SM-CLE-002", "厨房油污清洁剂 500ml", "泡沫喷雾，快速瓦解油污", "laundry-clean", "cleanhome", 16.90, 21.90, 170, "kitchen cleaning spray bottle", "油污清洁剂,厨房清洁,喷雾", "灶台,油烟机", "家庭", {"净含量":"500ml","形态":"泡沫喷雾","适用":"厨房硬表面"}),
    ("SM-PAP-001", "原生木浆抽纸 24包", "柔韧亲肤，家庭整箱", "paper-disposable", "softpaper", 39.90, 49.90, 155, "facial tissue box paper", "抽纸,纸巾,整箱", "客厅,餐桌,办公室", "家庭,办公", {"规格":"3层×100抽×24包","原料":"原生木浆","香型":"无香"}),
    ("SM-HAI-001", "去屑洗发水 700ml", "温和清洁，清爽头皮", "hair-body", "care", 39.90, 49.90, 112, "shampoo bottle bathroom", "洗发水,去屑,清爽", "日常洗护", "成人", {"净含量":"700ml","功效":"去屑清洁","适用发质":"中性/油性"}),
    ("SM-BOD-001", "清爽沐浴露 500ml", "细腻泡沫，洗后不紧绷", "hair-body", "care", 25.90, 32.90, 125, "body wash bottle", "沐浴露,清爽,保湿", "日常沐浴", "成人", {"净含量":"500ml","香型":"柚香","肤质":"通用"}),
    ("SM-ORA-001", "含氟清新牙膏 120g×2", "清洁牙渍，长效清新", "oral-hand", "care", 19.90, 25.90, 200, "toothpaste toothbrush", "牙膏,含氟,口腔护理", "早晚刷牙", "成人", {"规格":"120g×2","含氟量":"0.14%","香型":"薄荷"}),
    ("SM-COO-001", "不粘锅具两件套", "煎炒组合，少油易洁", "cookware", "home", 169.00, 219.00, 64, "nonstick frying pan", "煎锅,不粘锅,锅具套装", "煎炒,家庭烹饪", "家庭", {"件数":"2件","材质":"铝合金","适用炉灶":"燃气/电陶炉"}),
    ("SM-COO-002", "复古玻璃水杯 400ml", "通透杯身，红白几何纹样", "cookware", "home", 24.90, 32.90, 145, "glass drinking mug", "玻璃杯,水杯,复古", "居家,办公室", "学生,上班族", {"容量":"400ml","材质":"玻璃","图案":"红白几何纹"}),
    ("SM-STO-001", "加深塑料收纳篮 30L", "大开口设计，可分类叠放", "storage", "home", 39.90, 49.90, 78, "transparent storage box", "收纳篮,塑料,家居整理", "衣柜,储物间", "家庭", {"容量":"30L","材质":"PP","颜色":"橙色"}),
    ("SM-BAB-001", "婴儿纸尿片 M码 20片", "柔软吸收，日常便携装", "baby", "babycare", 39.00, 49.00, 92, "baby diapers package", "纸尿片,婴儿,M码", "婴儿日常护理", "6-11kg婴儿", {"尺码":"M","片数":"20片","适用体重":"6-11kg"}),
    ("SM-BAB-002", "婴儿手口湿巾 80抽×5", "无酒精无香精，柔软亲肤", "baby", "babycare", 29.90, 39.90, 118, "baby wet wipes package", "婴儿湿巾,手口,无酒精", "外出,喂养清洁", "婴幼儿", {"规格":"80抽×5包","成分":"EDI纯水","香精":"无"}),
    ("SM-PET-001", "成猫鸡肉配方粮 2kg", "均衡营养，添加牛磺酸", "pet", "petjoy", 69.00, 82.00, 84, "dry cat food bowl", "猫粮,成猫,鸡肉", "成猫日常主粮", "1岁以上猫", {"净含量":"2kg","适用阶段":"成猫","蛋白质":"≥30%"}),
    ("SM-APP-001", "自动断电电热水壶 1.7L", "大容量烧水，沸腾自动断电", "small-appliance", "smartlife", 109.00, 139.00, 56, "electric kettle kitchen", "电热水壶,自动断电,小家电", "居家,办公室", "家庭,上班族", {"容量":"1.7L","功率":"1500W","保护":"沸腾自动断电"}),
    ("SM-DIG-001", "蓝牙头戴式耳机", "舒适包耳，续航约40小时", "digital-accessory", "smartlife", 199.00, 249.00, 73, "wireless headphones", "蓝牙耳机,头戴式,长续航", "通勤,学习,音乐", "学生,上班族", {"连接":"Bluetooth 5.3","续航":"40小时","充电口":"USB-C"}),
    ("SM-DIG-002", "双向快充移动电源 10000mAh", "轻巧便携，双口输出", "digital-accessory", "smartlife", 99.00, 129.00, 101, "portable power bank", "移动电源,充电宝,快充", "通勤,旅行", "手机用户", {"容量":"10000mAh","输入":"USB-C","最大功率":"22.5W"}),
]

USERS = [
    {"username":"admin","nickname":"平台管理员","phone":None,"email":"admin@example.test","roles":["USER","ADMIN"]},
    {"username":"demo","nickname":"演示用户","phone":"18800000001","email":"demo@example.test","roles":["USER"]},
    {"username":"buyer01","nickname":"测试买家小林","phone":"18800000002","email":"buyer01@example.test","roles":["USER"]},
    {"username":"buyer02","nickname":"测试买家小周","phone":"18800000003","email":"buyer02@example.test","roles":["USER"]},
    {"username":"buyer03","nickname":"测试买家阿宁","phone":"18800000004","email":"buyer03@example.test","roles":["USER"]},
]

ADDRESSES = [
    ("demo","张测试","18800000001","上海市","上海市","浦东新区","张江路测试园区1号楼101室","200120",True,"家"),
    ("demo","张测试","18800000001","浙江省","杭州市","余杭区","文一西路测试公寓2幢202室","311100",False,"公司"),
    ("buyer01","林测试","18800000002","广东省","深圳市","南山区","科技园测试路8号3栋301室","518000",True,"家"),
    ("buyer01","林测试","18800000002","广东省","广州市","天河区","体育西测试街16号","510000",False,"父母家"),
    ("buyer02","周测试","18800000003","北京市","北京市","朝阳区","望京测试社区5号楼502室","100102",True,"家"),
    ("buyer02","周测试","18800000003","四川省","成都市","高新区","天府测试大道66号A座","610041",False,"公司"),
    ("buyer03","宁测试","18800000004","湖北省","武汉市","洪山区","光谷测试街18号6栋601室","430070",True,"家"),
    ("buyer03","宁测试","18800000004","江苏省","南京市","建邺区","江东测试路99号","210019",False,"亲友家"),
]

PREFERRED_FILE_TITLES = {
    "SM-GRA-001":"File:Rice Grains at Bakin Dogo Market Kaduna North.jpg",
    "SM-BOD-001":"File:Shower gel bottles.jpg",
    "SM-ORA-001":"File:舒客 Shuke cherry blossom enzyme toothpaste in a tube.jpg",
    "SM-COO-001":"File:T-fal Frying Pan at Lalaport Kadoma.jpg",
    "SM-COO-002":"File:Festival style drinking glass - 2023-11-29 - Andy Mabbett - 02.jpg",
    "SM-STO-001":"File:Gratnells Extra Deep Storage Tray.jpg",
    "SM-BAB-001":"File:UW 390323.jpg",
    "SM-PET-001":"File:Catfood.jpg",
    "SM-DIG-002":"File:Power bank.JPG",
}
REFRESH_IMAGES = set()
IMAGE_CATEGORIES = {
    "SM-FRU-001":"Apples", "SM-FRU-002":"Bananas", "SM-VEG-001":"Tomatoes",
    "SM-EGG-001":"Chicken eggs", "SM-DAI-001":"Milk", "SM-MEA-001":"Chicken meat",
    "SM-GRA-001":"Rice", "SM-GRA-002":"Flour", "SM-OIL-001":"Peanut oil",
    "SM-SNA-001":"Cookies", "SM-SNA-002":"Potato chips", "SM-NUT-001":"Almonds",
    "SM-DRI-001":"Bottled water", "SM-DRI-002":"Orange juice", "SM-COF-001":"Coffee beans",
    "SM-CLE-001":"Laundry detergents", "SM-CLE-002":"Cleaning agents", "SM-PAP-001":"Facial tissues",
    "SM-HAI-001":"Shampoo", "SM-BOD-001":"Shower gels", "SM-ORA-001":"Toothpaste",
    "SM-COO-001":"Frying pans", "SM-COO-002":"Drinking glasses", "SM-STO-001":"Plastic containers",
    "SM-BAB-001":"Disposable diapers", "SM-BAB-002":"Wet wipes", "SM-PET-001":"Cat food",
    "SM-APP-001":"Electric kettles", "SM-DIG-001":"Headphones", "SM-DIG-002":"Power banks",
}
IMAGE_SEARCH_TERMS = {
    "SM-FRU-001":"red apples", "SM-FRU-002":"banana bunch", "SM-VEG-001":"fresh tomatoes",
    "SM-EGG-001":"eggs carton", "SM-DAI-001":"glass milk", "SM-MEA-001":"raw chicken breast",
    "SM-GRA-001":"rice grains market", "SM-GRA-002":"flour bowl", "SM-OIL-001":"peanut oil bottle",
    "SM-SNA-001":"butter cookies", "SM-SNA-002":"potato chips bowl", "SM-NUT-001":"almond nuts",
    "SM-DRI-001":"mineral water bottles", "SM-DRI-002":"orange juice glass", "SM-COF-001":"roasted coffee beans",
    "SM-CLE-001":"detergent", "SM-CLE-002":"cleaner", "SM-PAP-001":"tissue",
    "SM-HAI-001":"shampoo", "SM-BOD-001":"shower gel", "SM-ORA-001":"toothpaste",
    "SM-COO-001":"frying pan", "SM-COO-002":"glass", "SM-STO-001":"plastic container",
    "SM-BAB-001":"diapers", "SM-BAB-002":"wet wipes", "SM-PET-001":"cat food",
    "SM-APP-001":"electric kettle", "SM-DIG-001":"wireless headphones", "SM-DIG-002":"portable power bank",
}

def clean(value: str | None) -> str:
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", "", value or ""))).strip()

def open_with_retry(request: urllib.request.Request, timeout: int):
    for attempt in range(6):
        try:
            return urllib.request.urlopen(request, timeout=timeout)
        except urllib.error.HTTPError as error:
            if error.code != 429 or attempt == 5:
                raise
            delay = int(error.headers.get("Retry-After", 0) or 0) or min(30, 4 * (attempt + 1))
            print(f"Wikimedia rate limit; retrying in {delay}s")
            time.sleep(delay)
        except (urllib.error.URLError, TimeoutError, ConnectionResetError) as error:
            if attempt == 5:
                raise
            delay = min(20, 3 * (attempt + 1))
            print(f"Network interruption ({error}); retrying in {delay}s")
            time.sleep(delay)

def commons_image(sku: str, query: str) -> dict:
    scoped_query = f'{IMAGE_SEARCH_TERMS[sku]} incategory:"{IMAGE_CATEGORIES[sku]}" filetype:bitmap'
    if sku in PREFERRED_FILE_TITLES:
        params = {"action":"query", "titles":PREFERRED_FILE_TITLES[sku], "prop":"imageinfo",
                  "iiprop":"url|mime|size|extmetadata", "iiurlwidth":"900", "format":"json", "formatversion":"2"}
    else:
        params = {"action":"query", "generator":"search", "gsrsearch":scoped_query, "gsrnamespace":"6",
                  "gsrlimit":"15", "prop":"imageinfo", "iiprop":"url|mime|size|extmetadata",
                  "iiurlwidth":"900", "format":"json", "formatversion":"2"}
    request = urllib.request.Request(API + "?" + urllib.parse.urlencode(params), headers={"User-Agent":USER_AGENT})
    with open_with_retry(request, timeout=30) as response:
        pages = sorted(json.load(response).get("query", {}).get("pages", []), key=lambda page: page.get("index", 9999))
    for page in pages:
        info = (page.get("imageinfo") or [{}])[0]
        metadata = info.get("extmetadata", {})
        license_name = clean(metadata.get("LicenseShortName", {}).get("value"))
        download_url = info.get("thumburl") or info.get("url")
        if info.get("mime") != "image/jpeg" or info.get("width", 0) < 500 or not download_url:
            continue
        return {
            "fileTitle":page["title"], "downloadUrl":download_url, "sourceUrl":info.get("descriptionurl"),
            "license":license_name, "licenseUrl":clean(metadata.get("LicenseUrl", {}).get("value")),
            "author":clean(metadata.get("Artist", {}).get("value")) or "Wikimedia Commons contributor",
            "credit":clean(metadata.get("Credit", {}).get("value")), "provider":"Wikimedia Commons",
            "source":"wikimedia", "mimeType":info.get("mime"),
        }
    raise RuntimeError(f"No compatible Commons photo found for {sku}: {scoped_query}")

def download(url: str, target: Path) -> None:
    time.sleep(0.35)
    request = urllib.request.Request(url, headers={"User-Agent":USER_AGENT})
    temporary = target.with_suffix(".download")
    with open_with_retry(request, timeout=90) as response, temporary.open("wb") as output:
        output.write(response.read())
    temporary.replace(target)

def main() -> None:
    IMAGE_DIR.mkdir(parents=True, exist_ok=True)
    attribution, products = [], []
    for index, row in enumerate(PRODUCTS, 1):
        sku, name, subtitle, category, brand, price, original, stock, query, keywords, scenarios, audiences, specs = row
        filename = f"{sku.lower()}.jpg"
        target = IMAGE_DIR / filename
        metadata_file = IMAGE_DIR / f"{sku.lower()}.source.json"
        if sku not in REFRESH_IMAGES and metadata_file.exists() and target.exists():
            source = json.loads(metadata_file.read_text("utf-8"))
        else:
            source = commons_image(sku, query)
            download(source["downloadUrl"], target)
            metadata_file.write_text(json.dumps(source, ensure_ascii=False, indent=2), "utf-8")
            time.sleep(0.35)
        source_record = {"sku":sku, "localFile":f"images/{filename}",
                         "fileSizeBytes":target.stat().st_size,
                         "sha256":hashlib.sha256(target.read_bytes()).hexdigest(), **source}
        attribution.append(source_record)
        products.append({
            "productNo":sku, "name":name, "subtitle":subtitle, "categoryCode":category, "brandCode":brand,
            "salePrice":f"{price:.2f}", "originalPrice":f"{original:.2f}", "stock":stock,
            "mainImageUrl":f"/uploads/seed/supermarket-v1/{filename}", "localImage":f"images/{filename}",
            "imageUrls":[], "description":f"{subtitle}。商品图片为真实商品类别摄影，商品名称、品牌、价格及规格均为测试数据。",
            "keywords":keywords, "scenarios":scenarios, "audiences":audiences, "specifications":specs,
            "status":"ON_SALE", "publishedOffsetDays":index,
        })
        print(f"[{index:02d}/{len(PRODUCTS)}] {sku} -> {source['fileTitle']}")

    dataset = {
        "manifest":{"id":"supermarket-v1", "version":"1.0.0", "locale":"zh-CN", "currency":"CNY",
                    "description":"综合超市第一阶段初始化测试数据；人物与地址均为虚构，图片来自 Wikimedia Commons 开放授权摄影资源。"},
        "categories":[{"code":c,"parentCode":p,"name":n,"level":level,"sortOrder":order,"status":"ENABLED"} for c,p,n,level,order in CATEGORIES],
        "brands":[{"code":c,"name":n,"description":d,"sortOrder":(i+1)*10,"status":"ENABLED"} for i,(c,n,d) in enumerate(BRANDS)],
        "users":USERS,
        "addresses":[{"username":u,"recipientName":r,"recipientPhone":ph,"province":pr,"city":ci,"district":di,"detailAddress":de,"postalCode":pc,"defaultAddress":default,"label":label} for u,r,ph,pr,ci,di,de,pc,default,label in ADDRESSES],
        "products":products,
    }
    OUTPUT.joinpath("dataset.json").write_text(json.dumps(dataset, ensure_ascii=False, indent=2), "utf-8")
    OUTPUT.joinpath("image-attribution.json").write_text(json.dumps(attribution, ensure_ascii=False, indent=2), "utf-8")
    print(f"Dataset written to {OUTPUT}")

if __name__ == "__main__":
    main()
