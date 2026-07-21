import { useEffect, useMemo, useState } from 'react';
import {
  Alert, App as AntApp, AutoComplete, Avatar, Badge, Button, Card, Checkbox, Col, Descriptions, Drawer, Empty, Flex, Form, Image,
  Input, InputNumber, Layout, List, Menu, message, Pagination, Popconfirm, Result, Row, Select, Skeleton, Slider,
  Space, Tag, Typography,
} from 'antd';
import {
  DeleteOutlined, FireOutlined, HeartFilled, HeartOutlined, HistoryOutlined, MenuOutlined, RobotOutlined,
  SearchOutlined, ShoppingCartOutlined, UserOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, Navigate, Outlet, Route, Routes, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { api, apiV2, AppError, formatPrice, type Product, type ProductSearch } from '@ai-commerce/api-client';

const { Header, Content, Footer } = Layout;
const { Title, Text, Paragraph } = Typography;

function useCurrentUser() {
  return useQuery({ queryKey: ['current-user'], queryFn: api.auth.me, retry: false });
}

function RequireAuth() {
  const user = useCurrentUser();
  const location = useLocation();
  if (user.isLoading) return <PageLoading />;
  if (!user.data) return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`} replace />;
  return <Outlet />;
}

function MallLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const user = useCurrentUser();
  const cart = useQuery({ queryKey: ['cart'], queryFn: api.cart.get, enabled: Boolean(user.data), retry: false });
  const [keyword, setKeyword] = useState('');
  const [suggestKeyword, setSuggestKeyword] = useState('');
  const [mobileOpen, setMobileOpen] = useState(false);
  useEffect(() => {
    const timer = window.setTimeout(() => setSuggestKeyword(keyword.trim()), 250);
    return () => window.clearTimeout(timer);
  }, [keyword]);
  const suggestions = useQuery({ queryKey: ['search-suggestions', suggestKeyword],
    queryFn: () => apiV2.search.suggestions(suggestKeyword, 8), enabled: suggestKeyword.length > 0, staleTime: 60_000 });
  const search = () => { if (keyword.trim()) navigate(`/products?keyword=${encodeURIComponent(keyword.trim())}`); };
  const logout = useMutation({ mutationFn: api.auth.logout, onSuccess: () => {
    queryClient.setQueryData(['current-user'], null); queryClient.removeQueries({ queryKey: ['cart'] }); navigate('/');
  }});

  useEffect(() => {
    const expired = () => queryClient.setQueryData(['current-user'], null);
    window.addEventListener('auth:expired', expired);
    return () => window.removeEventListener('auth:expired', expired);
  }, [queryClient]);

  const accountItems = user.data ? [
    { key: '/profile', label: <Link to="/profile">个人中心</Link> },
    { key: 'logout', label: '退出登录', onClick: () => logout.mutate() },
  ] : [{ key: '/login', label: <Link to="/login">登录 / 注册</Link> }];

  return <Layout className="site-layout">
    <Header className="site-header">
      <div className="header-inner">
        <Link to="/" className="brand">智选<span>商城</span></Link>
        <div className="header-search">
          <AutoComplete value={keyword} style={{ width: '100%' }} onSearch={setKeyword}
            onSelect={(_, option) => {
              const item = suggestions.data?.find((value) => `${value.type}:${value.id}` === option.key);
              if (!item) return;
              if (item.type === 'PRODUCT') navigate(`/products/${item.id}`);
              else if (item.type === 'CATEGORY') navigate(`/products?categoryId=${item.id}`);
              else navigate(`/products?brandId=${item.id}`);
            }} options={(suggestions.data || []).map((item) => ({ key: `${item.type}:${item.id}`, value: item.label,
              label: <Flex justify="space-between"><span>{item.label}</span><Text type="secondary">{item.type === 'PRODUCT' ? '商品' : item.type === 'CATEGORY' ? '分类' : '品牌'}</Text></Flex> }))}>
            <Input onPressEnter={search} prefix={<SearchOutlined />} placeholder="搜索商品、品牌或分类" suffix={<Button type="text" onClick={search}>搜索</Button>} />
          </AutoComplete>
        </div>
        <Space size="middle" className="desktop-actions">
          <Link to="/products">全部商品</Link>
          <Link to="/ai-search-demo"><RobotOutlined /> AI 导购</Link>
          {user.data && <Link to="/history"><HistoryOutlined /> 足迹</Link>}
          <Link to="/favorites"><HeartOutlined /> 收藏</Link>
          <Badge count={cart.data?.itemCount || 0} size="small"><Link to="/cart"><ShoppingCartOutlined /> 购物车</Link></Badge>
          <Menu mode="horizontal" selectable={false} items={[{ key: 'account', label: <span><UserOutlined /> {user.data?.nickname || '我的'}</span>, children: accountItems }]} />
        </Space>
        <Button className="mobile-menu" icon={<MenuOutlined />} onClick={() => setMobileOpen(true)} />
      </div>
    </Header>
    <Content className="site-content"><Outlet /></Content>
    <Footer className="site-footer">智选超市 · 一站式家庭采购平台</Footer>
    <Drawer open={mobileOpen} onClose={() => setMobileOpen(false)} title="导航">
      <Flex vertical gap={20} onClick={() => setMobileOpen(false)}>
        <Link to="/products">全部商品</Link><Link to="/ai-search-demo">AI 导购演示</Link><Link to="/history">浏览足迹</Link><Link to="/favorites">我的收藏</Link><Link to="/cart">购物车</Link>
        <Link to={user.data ? '/profile' : '/login'}>{user.data ? '个人中心' : '登录 / 注册'}</Link>
      </Flex>
    </Drawer>
  </Layout>;
}

function HomePage() {
  const navigate = useNavigate();
  const user = useCurrentUser();
  const [keyword, setKeyword] = useState('');
  const hot = useQuery({ queryKey: ['hot-products'], queryFn: () => apiV2.hot(undefined, 8) });
  const localHistoryIds = useMemo(() => readGuestHistory().map((item) => item.productId), []);
  const recent = useQuery({ queryKey: ['recent-viewed', Boolean(user.data)], queryFn: () => user.data
    ? apiV2.history.list(1, 8).then((value) => value.items.map((item) => item.product))
    : apiV2.search.batchSummary(localHistoryIds), enabled: Boolean(user.data) || localHistoryIds.length > 0 });
  const categories = useQuery({ queryKey: ['categories'], queryFn: api.catalog.categories });
  return <div>
    <section className="hero">
      <Tag color="blue">综合超市 · 全品类采购</Tag>
      <Title>日常所需，一站购齐</Title>
      <Paragraph>从生鲜食品、家庭清洁到母婴宠物和家电数码，按分类、品牌和预算快速选购。</Paragraph>
      <Input.Search size="large" value={keyword} onChange={(e) => setKeyword(e.target.value)}
        placeholder="例如：苹果、洗衣液、纸尿裤" enterButton="开始搜索"
        onSearch={(value) => value.trim() && navigate(`/products?keyword=${encodeURIComponent(value.trim())}`)} />
      <Space wrap className="category-chips">
        {categories.data?.filter((item) => item.level === 1).map((item) => <Button key={item.id} shape="round" onClick={() => navigate(`/products?categoryId=${item.id}`)}>{item.name}</Button>)}
      </Space>
    </section>
    <section className="container section">
      <div className="section-heading"><div><Text type="secondary">POPULAR NOW</Text><Title level={2}><FireOutlined /> 热销商品</Title></div><Link to="/products?sort=HOT">查看全部 →</Link></div>
      {hot.isLoading ? <ProductSkeleton /> : <ProductGrid products={hot.data?.items.map((item) => item.product) || []} hot />}
    </section>
    {(recent.data?.length || 0) > 0 && <section className="container section">
      <div className="section-heading"><div><Text type="secondary">RECENTLY VIEWED</Text><Title level={2}>最近浏览</Title></div>{user.data && <Link to="/history">查看足迹 →</Link>}</div>
      <ProductGrid products={recent.data || []} />
    </section>}
    <section className="container value-strip">
      <div><strong>严格商品管理</strong><span>价格、库存和状态实时校验</span></div>
      <div><strong>清晰筛选</strong><span>分类、品牌与预算一步到位</span></div>
      <div><strong>搜索能力升级</strong><span>全文检索、筛选与热销排序</span></div>
    </section>
  </div>;
}

function ProductsPage() {
  const [params, setParams] = useSearchParams();
  const [mobileFilter, setMobileFilter] = useState(false);
  const search = useMemo<ProductSearch>(() => ({
    keyword: params.get('keyword') || undefined,
    categoryId: params.get('categoryId') || undefined,
    brandId: params.get('brandId') || undefined,
    priceMin: params.get('priceMin') ? Number(params.get('priceMin')) : undefined,
    priceMax: params.get('priceMax') ? Number(params.get('priceMax')) : undefined,
    inStock: params.get('inStock') === 'true' || undefined,
    sort: (params.get('sort') as ProductSearch['sort']) || 'RELEVANCE',
    page: Number(params.get('page') || 1), pageSize: 12,
  }), [params]);
  const result = useQuery({ queryKey: ['products-v2', search], queryFn: () => apiV2.search.products(search) });
  const categories = useQuery({ queryKey: ['categories'], queryFn: api.catalog.categories });
  const brands = useQuery({ queryKey: ['brands'], queryFn: api.catalog.brands });
  const update = (values: Record<string, string | number | boolean | undefined>) => {
    const next = new URLSearchParams(params);
    Object.entries(values).forEach(([key, value]) => value === undefined || value === '' ? next.delete(key) : next.set(key, String(value)));
    if (!('page' in values)) next.set('page', '1');
    setParams(next);
  };
  const filters = <FilterPanel search={search} categories={categories.data || []} brands={brands.data || []}
    categoryFacets={result.data?.facets.categories || []} brandFacets={result.data?.facets.brands || []}
    update={update} clear={() => setParams({})} />;
  return <div className="container page-space">
    <div className="page-title-row"><div><Title level={2}>{search.keyword ? `“${search.keyword}”的搜索结果` : '全部商品'}</Title><Text type="secondary">共找到 {result.data?.total || 0} 件商品</Text></div>
      <Space><Button className="mobile-filter" onClick={() => setMobileFilter(true)}>筛选</Button><Select value={search.sort} style={{ width: 150 }} onChange={(sort) => update({ sort })}
        options={[{value:'RELEVANCE',label:'综合相关度'},{value:'PRICE_ASC',label:'价格从低到高'},{value:'PRICE_DESC',label:'价格从高到低'},{value:'NEWEST',label:'最新上架'},{value:'HOT',label:'热销优先'}]} /></Space></div>
    {result.data?.degraded && <Alert className="page-alert" type="warning" showIcon message="当前使用基础搜索模式，部分聚合与联想能力暂不可用" />}
    <div className="catalog-layout"><aside className="filters-desktop">{filters}</aside><main className="catalog-main">
      {result.isLoading ? <ProductSkeleton /> : result.data?.items.length ? <ProductGrid products={result.data.items} /> : <Empty description="没有找到符合条件的商品"><Button onClick={() => setParams({})}>清空筛选</Button></Empty>}
      <Pagination current={search.page} pageSize={12} total={result.data?.total || 0} hideOnSinglePage onChange={(page) => update({ page })} />
    </main></div>
    <Drawer title="筛选商品" open={mobileFilter} onClose={() => setMobileFilter(false)}>{filters}</Drawer>
  </div>;
}

function FilterPanel({ search, categories, brands, categoryFacets, brandFacets, update, clear }: { search: ProductSearch; categories: {id:string;name:string}[]; brands:{id:string;name:string}[]; categoryFacets:{id:string;name:string;count:number}[]; brandFacets:{id:string;name:string;count:number}[]; update:(v:Record<string,string|number|boolean|undefined>)=>void; clear:()=>void }) {
  const min = search.priceMin || 0, max = search.priceMax || 5000;
  const categoryCounts = new Map(categoryFacets.map((item) => [String(item.id), item.count]));
  const brandCounts = new Map(brandFacets.map((item) => [String(item.id), item.count]));
  return <Card title="筛选" extra={<Button type="link" onClick={clear}>清空</Button>} bordered={false}>
    <div className="filter-group"><Text strong>分类</Text><Select allowClear value={search.categoryId} placeholder="全部分类" options={categories.map(x => ({value:x.id,label:categoryCounts.has(String(x.id)) ? `${x.name} (${categoryCounts.get(String(x.id))})` : x.name}))} onChange={(categoryId) => update({categoryId})} /></div>
    <div className="filter-group"><Text strong>品牌</Text><Select allowClear value={search.brandId} placeholder="全部品牌" options={brands.map(x => ({value:x.id,label:brandCounts.has(String(x.id)) ? `${x.name} (${brandCounts.get(String(x.id))})` : x.name}))} onChange={(brandId) => update({brandId})} /></div>
    <div className="filter-group"><Text strong>价格区间</Text><Slider range min={0} max={10000} step={100} value={[min,max]} onChangeComplete={([priceMin,priceMax]) => update({priceMin,priceMax})} /><Text type="secondary">{formatPrice(min)} - {formatPrice(max)}</Text></div>
    <Checkbox checked={search.inStock} onChange={(e) => update({inStock:e.target.checked || undefined})}>仅看有货</Checkbox>
  </Card>;
}

function ProductDetailPage() {
  const { productId = '' } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useCurrentUser();
  const product = useQuery({ queryKey: ['product', productId], queryFn: () => api.catalog.product(productId) });
  const favorite = useQuery({ queryKey: ['favorite-status', productId], queryFn: () => api.favorites.status(productId), enabled: Boolean(user.data) });
  const cartMutation = useMutation({ mutationFn: (quantity: number) => api.cart.add(productId, quantity), onSuccess: (cart) => { queryClient.setQueryData(['cart'], cart); message.success('已加入购物车'); }, onError: showError });
  const favMutation = useMutation({ mutationFn: () => favorite.data?.favorite ? api.favorites.remove(productId) : api.favorites.add(productId), onSuccess: () => { queryClient.invalidateQueries({queryKey:['favorite-status',productId]}); queryClient.invalidateQueries({queryKey:['favorites']}); }, onError: showError });
  const [quantity, setQuantity] = useState(1);
  const clientViewId = useMemo(() => crypto.randomUUID(), [productId]);
  useEffect(() => {
    if (!product.data) return;
    if (!user.data) rememberGuestHistory(product.data.id);
    apiV2.history.record(product.data.id, clientViewId, getAnonymousId())
      .then(() => queryClient.invalidateQueries({ queryKey: ['recent-viewed'] }))
      .catch((error: unknown) => {
        console.error('[browsing-history] 浏览记录上报失败', {
          productId: product.data.id,
          clientViewId,
          authenticated: Boolean(user.data),
          error,
        });
        message.warning(user.data ? '浏览记录同步失败，请稍后重试' : '浏览记录暂未同步，已保存在本地');
      });
  }, [clientViewId, product.data, queryClient, user.data]);
  if (product.isLoading) return <PageLoading />;
  if (!product.data) return <Result status="404" title="商品不存在" extra={<Button onClick={() => navigate('/products')}>返回商品列表</Button>} />;
  const p = product.data;
  const specifications = parseSpecifications(p.specificationJson);
  const requireLogin = (action: () => void) => user.data ? action() : navigate(`/login?redirect=/products/${productId}`);
  return <div className="container page-space">
    <Row gutter={[48,32]}><Col xs={24} md={11}><Image className="detail-image" src={p.mainImageUrl} alt={p.name} /></Col><Col xs={24} md={13}>
      <Space wrap><Tag color="blue">{p.category.name}</Tag>{p.brand && <Tag>{p.brand.name}</Tag>}</Space>
      <Title>{p.name}</Title><Paragraph type="secondary" className="subtitle">{p.subtitle}</Paragraph>
      <div className="detail-price">{formatPrice(p.salePrice)} {p.originalPrice && <Text delete type="secondary">{formatPrice(p.originalPrice)}</Text>}</div>
      <Paragraph>{p.description}</Paragraph>
      <Space wrap>{p.keywords?.split(',').map((tag) => <Tag key={tag}>{tag}</Tag>)}</Space>
      <div className="purchase-box"><Text>{p.stock > 0 ? `现货 · 剩余 ${p.stock} 件` : '暂时缺货'}</Text><Space>
        <InputNumber min={1} max={p.stock} value={quantity} onChange={(v) => setQuantity(v || 1)} />
        <Button type="primary" icon={<ShoppingCartOutlined />} disabled={!p.stock} loading={cartMutation.isPending} onClick={() => requireLogin(() => cartMutation.mutate(quantity))}>加入购物车</Button>
        <Button icon={favorite.data?.favorite ? <HeartFilled /> : <HeartOutlined />} onClick={() => requireLogin(() => favMutation.mutate())}>{favorite.data?.favorite ? '已收藏' : '收藏'}</Button>
      </Space></div>
      <Card title="商品参数" className="spec-card">
        <Descriptions bordered size="middle" column={{ xs: 1, sm: 1, md: 2 }}>
          {specifications.map(({ label, value }) => <Descriptions.Item key={label} label={label}>{value}</Descriptions.Item>)}
          <Descriptions.Item label="适用场景">{p.scenarios || '日常使用'}</Descriptions.Item>
          <Descriptions.Item label="适用人群">{p.audiences || '通用'}</Descriptions.Item>
        </Descriptions>
      </Card>
    </Col></Row>
  </div>;
}

function CartPage() {
  const queryClient = useQueryClient();
  const cart = useQuery({ queryKey: ['cart'], queryFn: api.cart.get });
  const update = useMutation({ mutationFn: ({id,body}:{id:string;body:{quantity?:number;selected?:boolean}}) => api.cart.update(id, body), onSuccess: (data) => queryClient.setQueryData(['cart'], data), onError: showError });
  const remove = useMutation({ mutationFn: api.cart.remove, onSuccess: (data) => queryClient.setQueryData(['cart'], data), onError: showError });
  if (cart.isLoading) return <PageLoading />;
  if (!cart.data?.items.length) return <div className="container page-space"><Empty description="购物车还是空的"><Link to="/products"><Button type="primary">去逛逛</Button></Link></Empty></div>;
  return <div className="container page-space"><Title level={2}>我的购物车</Title><div className="cart-layout"><div>
    {cart.data.items.map((item) => <Card key={item.id} className="cart-item"><Flex gap={18} align="center" wrap>
      <Checkbox checked={item.selected} onChange={(e) => update.mutate({id:item.id,body:{selected:e.target.checked}})} />
      <Image width={110} height={110} preview={false} src={item.product.mainImageUrl} />
      <div className="cart-product"><Link to={`/products/${item.product.id}`}><Title level={4}>{item.product.name}</Title></Link><Text type={item.available?'secondary':'danger'}>{item.available?'有货':'商品已下架或库存不足'}</Text></div>
      <InputNumber min={1} max={item.product.stock} value={item.quantity} onChange={(quantity) => quantity && update.mutate({id:item.id,body:{quantity}})} />
      <Text strong>{formatPrice(item.subtotal)}</Text><Button danger type="text" onClick={() => remove.mutate(item.id)}>删除</Button>
    </Flex></Card>)}
  </div><Card className="cart-summary"><Title level={3}>购物车汇总</Title><Flex justify="space-between"><Text>已选商品</Text><Text>{cart.data.selectedCount} 件</Text></Flex><Flex justify="space-between"><Text strong>合计</Text><Title level={3}>{formatPrice(cart.data.selectedTotal)}</Title></Flex><Button type="primary" block disabled>订单功能后续开放</Button></Card></div></div>;
}

function FavoritesPage() {
  const queryClient = useQueryClient();
  const favorites = useQuery({ queryKey: ['favorites'], queryFn: () => api.favorites.list() });
  const remove = useMutation({ mutationFn: api.favorites.remove, onSuccess: () => queryClient.invalidateQueries({queryKey:['favorites']}), onError: showError });
  return <div className="container page-space"><Title level={2}>我的收藏</Title>{favorites.isLoading ? <ProductSkeleton /> : favorites.data?.items.length ?
    <Row gutter={[20,20]}>{favorites.data.items.map(({product}) => <Col xs={24} sm={12} lg={6} key={product.id}><ProductCard product={product} footer={<Button danger type="link" onClick={() => remove.mutate(product.id)}>取消收藏</Button>} /></Col>)}</Row>
    : <Empty description="暂未收藏商品" />}</div>;
}

function BrowsingHistoryPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const history = useQuery({ queryKey: ['browsing-history', page], queryFn: () => apiV2.history.list(page, 20) });
  const remove = useMutation({ mutationFn: apiV2.history.remove, onSuccess: () => {
    message.success('已删除该条足迹'); queryClient.invalidateQueries({ queryKey: ['browsing-history'] }); queryClient.invalidateQueries({ queryKey: ['recent-viewed'] });
  }, onError: showError });
  const clear = useMutation({ mutationFn: apiV2.history.clear, onSuccess: () => {
    message.success('浏览足迹已清空'); queryClient.invalidateQueries({ queryKey: ['browsing-history'] }); queryClient.invalidateQueries({ queryKey: ['recent-viewed'] });
  }, onError: showError });
  return <div className="container page-space">
    <div className="page-title-row"><div><Title level={2}><HistoryOutlined /> 浏览足迹</Title><Text type="secondary">记录你最近看过的商品，可随时删除</Text></div>
      <Popconfirm title="确认清空全部浏览足迹？" onConfirm={() => clear.mutate()}><Button danger icon={<DeleteOutlined />} disabled={!history.data?.items.length}>清空全部</Button></Popconfirm></div>
    {history.isLoading ? <ProductSkeleton /> : history.data?.items.length ? <>
      <List dataSource={history.data.items} renderItem={(item) => <List.Item actions={[
        <Button key="delete" danger type="text" onClick={() => remove.mutate(item.product.id)}>删除</Button>,
      ]}><List.Item.Meta avatar={<Image width={88} height={88} preview={false} src={item.product.mainImageUrl} />}
        title={<Link to={`/products/${item.product.id}`}>{item.product.name}</Link>}
        description={<Space direction="vertical" size={2}><Text>{formatPrice(item.product.salePrice)}</Text><Text type="secondary">最近浏览：{new Date(item.lastViewedAt).toLocaleString()} · 共 {item.viewCount} 次</Text></Space>} /></List.Item>} />
      <Pagination current={page} pageSize={20} total={history.data.total} hideOnSinglePage onChange={setPage} />
    </> : <Empty description="还没有浏览足迹"><Link to="/products"><Button type="primary">去逛逛</Button></Link></Empty>}
  </div>;
}

const demoScenarios = [
  { match: ['相机', '旅行'], summary: '为你模拟筛选了预算 500 元以内、适合旅行携带的相机类商品。', tags: ['相机', '旅行', '预算 ≤ ¥500'], products: [
    ['便携数码相机入门款', '¥399.00', '体积轻便，价格处于演示预算内'], ['儿童高清相机', '¥169.00', '操作简单，适合轻量记录'] ] },
  { match: ['零食', '低糖'], summary: '为你模拟筛选了办公室场景、低糖且 100 元以内的零食。', tags: ['零食', '办公室', '低糖', '预算 ≤ ¥100'], products: [
    ['每日坚果混合装', '¥49.90', '独立包装，适合办公室分享'], ['低糖全麦饼干', '¥19.90', '低糖偏好命中'] ] },
  { match: ['猫粮', '猫'], summary: '为你模拟筛选了适合一岁以上成猫的日常主粮。', tags: ['猫粮', '成猫', '1岁以上'], products: [
    ['高蛋白成猫粮 2kg', '¥69.00', '适用阶段和规格符合演示需求'], ['鲜肉成猫主粮 1.5kg', '¥82.00', '适合作为成猫日常主粮'] ] },
];

function AiDemoPage() {
  const [input, setInput] = useState('想买旅行用相机，预算500以内');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<typeof demoScenarios[number]>();
  const submit = () => {
    setLoading(true); setResult(undefined);
    window.setTimeout(() => {
      const matched = demoScenarios.find((scenario) => scenario.match.some((word) => input.includes(word)));
      setResult(matched || { match: [], summary: '这是无结果状态演示，请尝试示例问题。', tags: [], products: [] }); setLoading(false);
    }, 700);
  };
  return <div className="container narrow page-space ai-demo-page">
    <Alert type="info" showIcon icon={<RobotOutlined />} message="AI 导购交互原型" description="当前结果全部为本地 Mock 演示数据，尚未接入真实 AI 服务。" />
    <Card className="ai-demo-composer"><Title level={2}>告诉我你想买什么</Title><Paragraph type="secondary">可以描述预算、用途、使用人群或偏好。</Paragraph>
      <Input.Search size="large" value={input} onChange={(event) => setInput(event.target.value)} onSearch={submit} enterButton="模拟推荐" loading={loading} />
      <Space wrap className="category-chips">{['想买旅行用相机，预算500以内','适合办公室吃的低糖零食，100元以内','给一岁以上猫吃的猫粮'].map((text) => <Button key={text} size="small" onClick={() => setInput(text)}>{text}</Button>)}</Space>
    </Card>
    {loading && <Card><Skeleton active paragraph={{ rows: 4 }} /></Card>}
    {result && <Card title="Mock 推荐结果"><Paragraph>{result.summary}</Paragraph><Space wrap>{result.tags.map((tag) => <Tag color="blue" key={tag}>{tag}</Tag>)}</Space>
      {result.products.length ? <Row gutter={[16,16]} className="demo-products">{result.products.map(([name, price, reason]) => <Col xs={24} md={12} key={name}><Card size="small"><Title level={4}>{name}</Title><Text className="price">{price}</Text><Paragraph type="secondary">演示理由：{reason}</Paragraph></Card></Col>)}</Row> : <Empty description="Mock 场景暂无匹配商品" />}
    </Card>}
  </div>;
}

function ProfilePage() {
  const user = useCurrentUser();
  return <div className="container narrow page-space"><Card><Flex vertical align="center" gap={16}><Avatar size={88} icon={<UserOutlined />} /><Title level={2}>{user.data?.nickname}</Title><Text type="secondary">账号：{user.data?.username}</Text><Tag color="blue">普通用户</Tag><Link to="/history"><Button icon={<HistoryOutlined />}>查看浏览足迹</Button></Link></Flex></Card></div>;
}

function LoginPage() {
  const navigate = useNavigate(); const [params] = useSearchParams(); const queryClient = useQueryClient();
  const { modal, message: appMessage } = AntApp.useApp();
  const login = useMutation({ mutationFn: api.auth.login, onSuccess: (user) => {
    queryClient.setQueryData(['current-user'], user);
    const redirect = params.get('redirect') || '/';
    const guestHistory = readGuestHistory();
    if (!guestHistory.length) {
      navigate(redirect);
      return;
    }
    modal.confirm({
      title: '是否合并游客浏览足迹？',
      content: `检测到 ${guestHistory.length} 条登录前的浏览记录，合并后可在“浏览足迹”中继续查看。`,
      okText: '合并足迹',
      cancelText: '暂不合并',
      maskClosable: false,
      onOk: async () => {
        try {
          await apiV2.history.merge(guestHistory);
          clearGuestHistory();
          queryClient.removeQueries({ queryKey: ['recent-viewed', false] });
          await Promise.all([
            queryClient.invalidateQueries({ queryKey: ['browsing-history'] }),
            queryClient.invalidateQueries({ queryKey: ['recent-viewed'] }),
          ]);
          appMessage.success(`已合并 ${guestHistory.length} 条浏览足迹`);
          navigate(redirect);
        } catch (error) {
          console.error('[browsing-history] 游客足迹合并失败', { count: guestHistory.length, error });
          showError(error instanceof Error ? error : new Error('游客足迹合并失败'));
          throw error;
        }
      },
      onCancel: () => navigate(redirect),
    });
  }, onError: showError });
  return <AuthShell title="欢迎回来" subtitle="登录后管理收藏和购物车"><Form layout="vertical" onFinish={(v) => login.mutate(v)}>
    <Form.Item name="account" label="账号" rules={[{required:true,message:'请输入用户名、手机号或邮箱'}]}><Input autoComplete="username" /></Form.Item>
    <Form.Item name="password" label="密码" rules={[{required:true,message:'请输入密码'}]}><Input.Password autoComplete="current-password" /></Form.Item>
    <Button htmlType="submit" type="primary" block loading={login.isPending}>登录</Button>
    <Paragraph className="auth-switch">还没有账号？<Link to="/register">立即注册</Link></Paragraph>
    <Text type="secondary">演示用户：demo / User@123456</Text>
  </Form></AuthShell>;
}

function RegisterPage() {
  const navigate = useNavigate(); const register = useMutation({ mutationFn: api.auth.register, onSuccess: () => { message.success('注册成功，请登录'); navigate('/login'); }, onError: showError });
  return <AuthShell title="创建账号" subtitle="注册后即可收藏商品和使用购物车"><Form layout="vertical" onFinish={(v) => register.mutate(v)}>
    <Form.Item name="username" label="用户名" rules={[{required:true,min:3,max:64}]}><Input autoComplete="username" /></Form.Item>
    <Form.Item name="phone" label="手机号（选填）"><Input /></Form.Item><Form.Item name="email" label="邮箱（选填）" rules={[{type:'email'}]}><Input /></Form.Item>
    <Form.Item name="password" label="密码" rules={[{required:true,min:8,message:'密码至少8位'}]}><Input.Password autoComplete="new-password" /></Form.Item>
    <Button htmlType="submit" type="primary" block loading={register.isPending}>注册</Button><Paragraph className="auth-switch">已有账号？<Link to="/login">去登录</Link></Paragraph>
  </Form></AuthShell>;
}

function AuthShell({title,subtitle,children}:{title:string;subtitle:string;children:React.ReactNode}) { return <div className="auth-page"><Card className="auth-card"><Title level={2}>{title}</Title><Paragraph type="secondary">{subtitle}</Paragraph>{children}</Card></div>; }

function ProductGrid({ products, hot = false }: { products: Product[]; hot?: boolean }) { return <Row gutter={[20,24]}>{products.map((product) => <Col xs={24} sm={12} lg={6} key={product.id}><ProductCard product={product} hot={hot} /></Col>)}</Row>; }
function ProductCard({ product, footer, hot = false }: { product: Product; footer?: React.ReactNode; hot?: boolean }) { return <Card className="product-card" hoverable cover={<Link to={`/products/${product.id}`}><img src={product.mainImageUrl} alt={product.name} /></Link>}>
  <Space wrap>{hot && <Tag color="red" icon={<FireOutlined />}>热销</Tag>}<Tag color="blue">{product.category.name}</Tag>{product.brand && <Tag>{product.brand.name}</Tag>}</Space><Link to={`/products/${product.id}`}><Title level={4} ellipsis={{rows:1}}>{product.name}</Title></Link><Paragraph type="secondary" ellipsis={{rows:2}}>{product.subtitle}</Paragraph><Flex justify="space-between" align="center"><Text className="price">{formatPrice(product.salePrice)}</Text><Text type={product.stock?'secondary':'danger'}>{product.stock?'有货':'缺货'}</Text></Flex>{footer}
</Card>; }
function ProductSkeleton() { return <Row gutter={[20,20]}>{[1,2,3,4].map((i) => <Col xs={24} sm={12} lg={6} key={i}><Card><Skeleton active /></Card></Col>)}</Row>; }
function PageLoading() { return <div className="container page-space"><Skeleton active paragraph={{rows:8}} /></div>; }
function parseSpecifications(value?: string): { label: string; value: string }[] {
  if (!value?.trim()) return [];
  try {
    const parsed: unknown = JSON.parse(value);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return [{ label: '规格说明', value: String(parsed ?? '') }];
    return Object.entries(parsed as Record<string, unknown>).map(([label, item]) => ({
      label,
      value: item !== null && typeof item === 'object' ? JSON.stringify(item) : String(item ?? '-'),
    }));
  } catch {
    return [{ label: '规格说明', value }];
  }
}
function showError(error: Error) { message.error(error instanceof AppError ? error.message : '操作失败，请重试'); }

type GuestHistoryItem = { productId: string; lastViewedAt: string };
function readGuestHistory(): GuestHistoryItem[] {
  try { return JSON.parse(localStorage.getItem('commerce:guest-history:v2') || '[]') as GuestHistoryItem[]; } catch { return []; }
}
function rememberGuestHistory(productId: string) {
  const next = [{ productId, lastViewedAt: new Date().toISOString() }, ...readGuestHistory().filter((item) => item.productId !== productId)].slice(0, 20);
  localStorage.setItem('commerce:guest-history:v2', JSON.stringify(next));
}
function clearGuestHistory() {
  localStorage.removeItem('commerce:guest-history:v2');
}
function getAnonymousId() {
  const key = 'commerce:anonymous-id:v2';
  let value = localStorage.getItem(key);
  if (!value) { value = crypto.randomUUID(); localStorage.setItem(key, value); }
  return value;
}

export default function App() {
  return <AntApp><Routes>
    <Route element={<MallLayout />}>
      <Route index element={<HomePage />} /><Route path="products" element={<ProductsPage />} /><Route path="products/:productId" element={<ProductDetailPage />} />
      <Route path="ai-search-demo" element={<AiDemoPage />} />
      <Route path="login" element={<LoginPage />} /><Route path="register" element={<RegisterPage />} />
      <Route element={<RequireAuth />}><Route path="cart" element={<CartPage />} /><Route path="favorites" element={<FavoritesPage />} /><Route path="history" element={<BrowsingHistoryPage />} /><Route path="profile" element={<ProfilePage />} /></Route>
      <Route path="*" element={<Result status="404" title="页面不存在" extra={<Link to="/"><Button type="primary">返回首页</Button></Link>} />} />
    </Route>
  </Routes></AntApp>;
}
