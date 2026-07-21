import { useEffect, useMemo, useState } from 'react';
import {
  App as AntApp, Button, Card, Col, Descriptions, Flex, Form, Image, Input, InputNumber, Layout, Menu,
  message, Modal, Popconfirm, Result, Row, Select, Space, Statistic, Table, Tag, Typography,
} from 'antd';
import {
  AppstoreOutlined, DashboardOutlined, LogoutOutlined, PlusOutlined, ProductOutlined, TagsOutlined, UserOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, Navigate, Outlet, Route, Routes, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { api, AppError, formatPrice, type Brand, type Category, type Product, type ProductInput, type ProductStatus } from '@ai-commerce/api-client';

const { Sider, Header, Content } = Layout;
const { Title, Text, Paragraph } = Typography;

function useAdmin() { return useQuery({ queryKey: ['current-user'], queryFn: api.auth.me, retry: false }); }
function AdminGuard() { const user = useAdmin(); const location = useLocation(); if (user.isLoading) return <div className="center-page">正在加载...</div>; if (!user.data) return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace/>; if (!user.data.roles.includes('ADMIN')) return <Result status="403" title="无权访问管理后台"/>; return <Outlet/>; }

function AdminLayout() {
  const location = useLocation(); const navigate = useNavigate(); const queryClient = useQueryClient(); const user = useAdmin();
  const logout = useMutation({ mutationFn: api.auth.logout, onSuccess: () => { queryClient.clear(); navigate('/login'); } });
  return <Layout className="admin-shell"><Sider width={240} breakpoint="lg" collapsedWidth="0" className="admin-sider">
    <div className="admin-logo"><span>智选</span>管理台</div>
    <Menu theme="dark" mode="inline" selectedKeys={[location.pathname]} onClick={({key}) => navigate(key)} items={[
      {key:'/',icon:<DashboardOutlined/>,label:'数据概览'}, {key:'/products',icon:<ProductOutlined/>,label:'商品管理'},
      {key:'/categories',icon:<AppstoreOutlined/>,label:'分类管理'}, {key:'/brands',icon:<TagsOutlined/>,label:'品牌管理'},
    ]}/>
  </Sider><Layout><Header className="admin-header"><div><Text type="secondary">AI 商品采购平台</Text></div><Space><UserOutlined/><Text>{user.data?.nickname}</Text><Button type="text" icon={<LogoutOutlined/>} onClick={() => logout.mutate()}>退出</Button></Space></Header>
    <Content className="admin-content"><Outlet/></Content></Layout></Layout>;
}

function LoginPage() {
  const navigate = useNavigate(); const [params] = useSearchParams(); const queryClient = useQueryClient();
  const login = useMutation({ mutationFn: api.auth.login, onSuccess: (user) => { if (!user.roles.includes('ADMIN')) { message.error('该账号不是管理员'); return; } queryClient.setQueryData(['current-user'],user); navigate(params.get('redirect')||'/'); }, onError: showError });
  return <div className="login-page"><Card className="login-card"><div className="login-brand">智选<span>管理台</span></div><Title level={2}>管理员登录</Title><Paragraph type="secondary">维护商品、分类、品牌和库存</Paragraph><Form layout="vertical" initialValues={{account:'admin',password:'Admin@123456'}} onFinish={(v)=>login.mutate(v)}>
    <Form.Item name="account" label="管理员账号" rules={[{required:true}]}><Input autoComplete="username"/></Form.Item><Form.Item name="password" label="密码" rules={[{required:true}]}><Input.Password autoComplete="current-password"/></Form.Item><Button block type="primary" htmlType="submit" loading={login.isPending}>进入管理后台</Button>
  </Form><Text type="secondary">默认演示账号：admin / Admin@123456</Text></Card></div>;
}

function DashboardPage() {
  const products = useQuery({ queryKey:['admin-products','summary'], queryFn:()=>api.admin.products({page:1,pageSize:100}) });
  const categories = useQuery({ queryKey:['admin-categories'],queryFn:api.admin.categories }); const brands = useQuery({queryKey:['admin-brands'],queryFn:api.admin.brands});
  const onSale = products.data?.items.filter(x=>x.status==='ON_SALE').length || 0; const lowStock = products.data?.items.filter(x=>x.stock<10).length || 0;
  return <div><div className="admin-page-title"><div><Title level={2}>数据概览</Title><Text type="secondary">第一阶段商城运行概况</Text></div></div><Row gutter={[20,20]}>
    <Col xs={24} sm={12} xl={6}><Card><Statistic title="商品总数" value={products.data?.total||0}/></Card></Col><Col xs={24} sm={12} xl={6}><Card><Statistic title="在售商品" value={onSale}/></Card></Col><Col xs={24} sm={12} xl={6}><Card><Statistic title="库存预警" value={lowStock} valueStyle={{color:lowStock?'#ba1a1a':undefined}}/></Card></Col><Col xs={24} sm={12} xl={6}><Card><Statistic title="分类 / 品牌" value={`${categories.data?.length||0} / ${brands.data?.length||0}`}/></Card></Col>
  </Row><Card className="guide-card" title="快速开始"><Space wrap><Link to="/products/new"><Button type="primary" icon={<PlusOutlined/>}>新增商品</Button></Link><Link to="/categories"><Button>维护分类</Button></Link><Link to="/brands"><Button>维护品牌</Button></Link></Space></Card></div>;
}

function ProductsPage() {
  const navigate = useNavigate(); const queryClient = useQueryClient(); const [params,setParams]=useSearchParams(); const keyword=params.get('keyword')||''; const status=(params.get('status') as ProductStatus)||undefined; const page=Number(params.get('page')||1);
  const result=useQuery({queryKey:['admin-products',{keyword,status,page}],queryFn:()=>api.admin.products({keyword,status,page,pageSize:20})});
  const statusMutation=useMutation({mutationFn:({p,status}:{p:Product;status:ProductStatus})=>api.admin.status(p.id,status,p.version),onSuccess:()=>queryClient.invalidateQueries({queryKey:['admin-products']}),onError:showError});
  const deleteMutation=useMutation({mutationFn:(p:Product)=>api.admin.deleteProduct(p.id,p.version),onSuccess:()=>queryClient.invalidateQueries({queryKey:['admin-products']}),onError:showError});
  const columns=[
    {title:'商品',dataIndex:'name',render:(_:unknown,p:Product)=><Flex gap={12} align="center"><Image width={54} height={54} preview={false} src={p.mainImageUrl}/><div><Text strong>{p.name}</Text><br/><Text type="secondary">{p.productNo}</Text></div></Flex>},
    {title:'分类 / 品牌',render:(_:unknown,p:Product)=><span>{p.category.name} / {p.brand?.name||'-'}</span>},{title:'价格',dataIndex:'salePrice',render:formatPrice},{title:'库存',dataIndex:'stock',render:(v:number)=><Text type={v<10?'danger':undefined}>{v}</Text>},
    {title:'状态',dataIndex:'status',render:(v:ProductStatus)=><StatusTag status={v}/>},{title:'操作',fixed:'right' as const,render:(_:unknown,p:Product)=><Space><Button type="link" onClick={()=>navigate(`/products/${p.id}/edit`)}>编辑</Button><Button type="link" onClick={()=>statusMutation.mutate({p,status:p.status==='ON_SALE'?'OFF_SALE':'ON_SALE'})}>{p.status==='ON_SALE'?'下架':'上架'}</Button><Popconfirm title="确认删除该商品？" onConfirm={()=>deleteMutation.mutate(p)}><Button danger type="link">删除</Button></Popconfirm></Space>},
  ];
  const update=(key:string,value:string)=>{const next=new URLSearchParams(params);value?next.set(key,value):next.delete(key);next.set('page','1');setParams(next);};
  return <div><div className="admin-page-title"><div><Title level={2}>商品管理</Title><Text type="secondary">维护商品信息、库存与上下架状态</Text></div><Button type="primary" icon={<PlusOutlined/>} onClick={()=>navigate('/products/new')}>新增商品</Button></div>
    <Card><Flex gap={12} wrap className="table-tools"><Input.Search allowClear defaultValue={keyword} placeholder="商品名称或编号" onSearch={(v)=>update('keyword',v)} style={{width:280}}/><Select allowClear value={status} placeholder="全部状态" style={{width:150}} onChange={(v)=>update('status',v||'')} options={[{value:'DRAFT',label:'草稿'},{value:'ON_SALE',label:'已上架'},{value:'OFF_SALE',label:'已下架'}]}/></Flex>
      <Table rowKey="id" loading={result.isLoading} dataSource={result.data?.items||[]} columns={columns} scroll={{x:900}} pagination={{current:page,pageSize:20,total:result.data?.total||0,onChange:(value)=>{const next=new URLSearchParams(params);next.set('page',String(value));setParams(next);}}}/></Card></div>;
}

function ProductFormPage() {
  const {id}=useParams(); const editing=Boolean(id); const navigate=useNavigate(); const [form]=Form.useForm<ProductInput>();
  const product=useQuery({queryKey:['admin-product',id],queryFn:()=>api.admin.product(id!),enabled:editing}); const categories=useQuery({queryKey:['admin-categories'],queryFn:api.admin.categories}); const brands=useQuery({queryKey:['admin-brands'],queryFn:api.admin.brands});
  useEffect(()=>{if(product.data) form.setFieldsValue({...product.data,categoryId:product.data.category.id,brandId:product.data.brand?.id,specificationJson:product.data.specificationJson||'{}',imageUrlsJson:product.data.imageUrlsJson||'[]'});},[product.data,form]);
  const save=useMutation({mutationFn:(body:ProductInput)=>editing?api.admin.updateProduct(id!,{...body,version:product.data?.version}):api.admin.createProduct(body),onSuccess:()=>{message.success('商品已保存');navigate('/products');},onError:showError});
  return <div><div className="admin-page-title"><div><Title level={2}>{editing?'编辑商品':'新增商品'}</Title><Text type="secondary">完善结构化信息，为后续智能搜索做好准备</Text></div><Button onClick={()=>navigate('/products')}>返回列表</Button></div><Card loading={editing&&product.isLoading}><Form form={form} layout="vertical" initialValues={{status:'DRAFT',stock:0,specificationJson:'{}',imageUrlsJson:'[]'}} onFinish={(v)=>save.mutate(v)}>
    <SectionTitle title="基础信息"/><Row gutter={20}><Col xs={24} md={8}><Form.Item name="productNo" label="商品编号" rules={[{required:true}]}><Input/></Form.Item></Col><Col xs={24} md={16}><Form.Item name="name" label="商品名称" rules={[{required:true,max:200}]}><Input/></Form.Item></Col><Col span={24}><Form.Item name="subtitle" label="商品副标题"><Input/></Form.Item></Col><Col xs={24} md={12}><Form.Item name="categoryId" label="分类" rules={[{required:true}]}><Select options={categories.data?.filter(x=>x.status==='ENABLED').map(x=>({value:x.id,label:x.name}))}/></Form.Item></Col><Col xs={24} md={12}><Form.Item name="brandId" label="品牌"><Select allowClear options={brands.data?.filter(x=>x.status==='ENABLED').map(x=>({value:x.id,label:x.name}))}/></Form.Item></Col></Row>
    <SectionTitle title="销售与库存"/><Row gutter={20}><Col xs={24} md={8}><Form.Item name="salePrice" label="销售价" rules={[{required:true}]}><InputNumber min={0} precision={2} style={{width:'100%'}}/></Form.Item></Col><Col xs={24} md={8}><Form.Item name="originalPrice" label="原价"><InputNumber min={0} precision={2} style={{width:'100%'}}/></Form.Item></Col><Col xs={24} md={8}><Form.Item name="stock" label="库存" rules={[{required:true}]}><InputNumber min={0} precision={0} style={{width:'100%'}}/></Form.Item></Col></Row>
    <SectionTitle title="媒体与描述"/><Form.Item name="mainImageUrl" label="商品主图 URL" rules={[{required:true,type:'url'}]}><Input/></Form.Item><Form.Item name="description" label="商品描述" rules={[{required:true}]}><Input.TextArea rows={5}/></Form.Item>
    <SectionTitle title="搜索与结构化信息"/><Row gutter={20}><Col xs={24} md={8}><Form.Item name="keywords" label="关键词（逗号分隔）"><Input/></Form.Item></Col><Col xs={24} md={8}><Form.Item name="scenarios" label="使用场景"><Input/></Form.Item></Col><Col xs={24} md={8}><Form.Item name="audiences" label="适用人群"><Input/></Form.Item></Col></Row><Form.Item name="specificationJson" label="规格参数 JSON" rules={[{validator:(_,v)=>jsonRule(v)}]}><Input.TextArea rows={5}/></Form.Item>
    <Form.Item name="imageUrlsJson" hidden><Input/></Form.Item><SectionTitle title="发布状态"/><Form.Item name="status" label="商品状态" rules={[{required:true}]}><Select style={{width:220}} options={[{value:'DRAFT',label:'草稿'},{value:'ON_SALE',label:'立即上架'},{value:'OFF_SALE',label:'下架'}]}/></Form.Item><Space><Button type="primary" htmlType="submit" loading={save.isPending}>保存商品</Button><Button onClick={()=>navigate('/products')}>取消</Button></Space>
  </Form></Card></div>;
}

function CategoriesPage() {
  const queryClient=useQueryClient(); const list=useQuery({queryKey:['admin-categories'],queryFn:api.admin.categories}); const [editing,setEditing]=useState<Category|null|undefined>(); const [form]=Form.useForm();
  const save=useMutation({mutationFn:(v:Omit<Category,'id'|'level'>)=>api.admin.saveCategory(editing?.id,v),onSuccess:()=>{message.success('分类已保存');setEditing(undefined);queryClient.invalidateQueries({queryKey:['admin-categories']});},onError:showError}); const remove=useMutation({mutationFn:api.admin.deleteCategory,onSuccess:()=>queryClient.invalidateQueries({queryKey:['admin-categories']}),onError:showError});
  const open=(value:Category|null)=>{setEditing(value);form.setFieldsValue(value||{status:'ENABLED',sortOrder:0,parentId:undefined});};
  return <div><div className="admin-page-title"><div><Title level={2}>分类管理</Title><Text type="secondary">支持两级商品分类</Text></div><Button type="primary" icon={<PlusOutlined/>} onClick={()=>open(null)}>新增分类</Button></div><Card><Table rowKey="id" dataSource={list.data||[]} loading={list.isLoading} pagination={false} columns={[{title:'分类名称',dataIndex:'name'},{title:'层级',dataIndex:'level'},{title:'排序',dataIndex:'sortOrder'},{title:'状态',dataIndex:'status',render:(v)=><Tag color={v==='ENABLED'?'green':'default'}>{v==='ENABLED'?'启用':'禁用'}</Tag>},{title:'操作',render:(_,v:Category)=><Space><Button type="link" onClick={()=>open(v)}>编辑</Button><Popconfirm title="确认删除？" onConfirm={()=>remove.mutate(v.id)}><Button danger type="link">删除</Button></Popconfirm></Space>} ]}/></Card>
    <Modal open={editing!==undefined} title={editing?.id?'编辑分类':'新增分类'} onCancel={()=>setEditing(undefined)} onOk={()=>form.submit()} confirmLoading={save.isPending}><Form form={form} layout="vertical" onFinish={(v)=>save.mutate(v)}><Form.Item name="name" label="名称" rules={[{required:true}]}><Input/></Form.Item><Form.Item name="parentId" label="父分类"><Select allowClear options={list.data?.filter(x=>x.level===1&&x.id!==editing?.id).map(x=>({value:x.id,label:x.name}))}/></Form.Item><Form.Item name="sortOrder" label="排序"><InputNumber min={0}/></Form.Item><Form.Item name="status" label="状态"><Select options={[{value:'ENABLED',label:'启用'},{value:'DISABLED',label:'禁用'}]}/></Form.Item></Form></Modal>
  </div>;
}

function BrandsPage() {
  const queryClient=useQueryClient(); const list=useQuery({queryKey:['admin-brands'],queryFn:api.admin.brands}); const [editing,setEditing]=useState<Brand|null|undefined>(); const [form]=Form.useForm();
  const save=useMutation({mutationFn:(v:Omit<Brand,'id'>)=>api.admin.saveBrand(editing?.id,v),onSuccess:()=>{message.success('品牌已保存');setEditing(undefined);queryClient.invalidateQueries({queryKey:['admin-brands']});},onError:showError}); const remove=useMutation({mutationFn:api.admin.deleteBrand,onSuccess:()=>queryClient.invalidateQueries({queryKey:['admin-brands']}),onError:showError});
  const open=(value:Brand|null)=>{setEditing(value);form.setFieldsValue(value||{status:'ENABLED',sortOrder:0});};
  return <div><div className="admin-page-title"><div><Title level={2}>品牌管理</Title><Text type="secondary">维护商品品牌及基础信息</Text></div><Button type="primary" icon={<PlusOutlined/>} onClick={()=>open(null)}>新增品牌</Button></div><Card><Table rowKey="id" dataSource={list.data||[]} loading={list.isLoading} pagination={false} columns={[{title:'品牌',dataIndex:'name'},{title:'说明',dataIndex:'description'},{title:'排序',dataIndex:'sortOrder'},{title:'状态',dataIndex:'status',render:(v)=><Tag color={v==='ENABLED'?'green':'default'}>{v==='ENABLED'?'启用':'禁用'}</Tag>},{title:'操作',render:(_,v:Brand)=><Space><Button type="link" onClick={()=>open(v)}>编辑</Button><Popconfirm title="确认删除？" onConfirm={()=>remove.mutate(v.id)}><Button danger type="link">删除</Button></Popconfirm></Space>} ]}/></Card>
    <Modal open={editing!==undefined} title={editing?.id?'编辑品牌':'新增品牌'} onCancel={()=>setEditing(undefined)} onOk={()=>form.submit()} confirmLoading={save.isPending}><Form form={form} layout="vertical" onFinish={(v)=>save.mutate(v)}><Form.Item name="name" label="名称" rules={[{required:true}]}><Input/></Form.Item><Form.Item name="logoUrl" label="Logo URL"><Input/></Form.Item><Form.Item name="description" label="说明"><Input.TextArea/></Form.Item><Form.Item name="sortOrder" label="排序"><InputNumber min={0}/></Form.Item><Form.Item name="status" label="状态"><Select options={[{value:'ENABLED',label:'启用'},{value:'DISABLED',label:'禁用'}]}/></Form.Item></Form></Modal>
  </div>;
}

function StatusTag({status}:{status:ProductStatus}) { const map={DRAFT:['default','草稿'],ON_SALE:['green','已上架'],OFF_SALE:['orange','已下架']} as const; return <Tag color={map[status][0]}>{map[status][1]}</Tag>; }
function SectionTitle({title}:{title:string}) { return <Title level={4} className="form-section">{title}</Title>; }
function jsonRule(value?:string) { if(!value) return Promise.resolve(); try{JSON.parse(value);return Promise.resolve();}catch{return Promise.reject(new Error('请输入有效 JSON'));} }
function showError(error:Error) { message.error(error instanceof AppError?error.message:'操作失败，请重试'); }

export default function App(){return <AntApp><Routes><Route path="/login" element={<LoginPage/>}/><Route element={<AdminGuard/>}><Route element={<AdminLayout/>}><Route index element={<DashboardPage/>}/><Route path="products" element={<ProductsPage/>}/><Route path="products/new" element={<ProductFormPage/>}/><Route path="products/:id/edit" element={<ProductFormPage/>}/><Route path="categories" element={<CategoriesPage/>}/><Route path="brands" element={<BrandsPage/>}/></Route></Route><Route path="*" element={<Result status="404" title="页面不存在" extra={<Link to="/"><Button>返回后台首页</Button></Link>}/>}/></Routes></AntApp>}
