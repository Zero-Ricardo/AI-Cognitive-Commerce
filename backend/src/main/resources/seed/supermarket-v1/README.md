# supermarket-v1 初始化数据集

该目录是综合超市第一版可重复导入的初始化数据集。

## 数据规模

- 10 个一级分类、20 个二级分类
- 15 个虚构测试品牌
- 30 个详细商品
- 5 个测试用户
- 8 条虚构收货地址
- 30 张本地商品实拍图

## 文件说明

- `dataset.json`：分类、品牌、商品、用户与收货地址数据。
- `images/*.jpg`：由构建脚本下载到本地的商品类目实拍图。
- `images/*.source.json`：单张图片的作者、来源、许可证和原始下载地址。
- `image-attribution.json`：全部图片授权信息汇总。

商品名称、品牌、价格、库存、规格、用户和地址均为测试数据，不对应真实在售商品或真实个人。图片仅用于表达商品类别，不表示摄影作品中的真实品牌与测试品牌存在关系。

## 重新构建

在 `backend` 目录执行：

```bash
python3 scripts/build_supermarket_dataset.py
```

脚本通过 Wikimedia Commons API 检索开放授权图片。已核验的图片默认从本地复用；需要替换图片时，将对应 SKU 加入脚本中的 `REFRESH_IMAGES`。

## 数据库导入

Spring Boot 启动时读取 `dataset.json`，通过 `seed_datasets` 表保证只导入一次。图片同时复制到：

```text
${app.storage.path}/seed/supermarket-v1/
```

旧版仅包含 6 条数码演示商品时，导入器会安全替换旧目录数据；检测到其他业务商品时不会执行清理。
