import axios, { AxiosError, type AxiosInstance } from 'axios';

export type Role = 'USER' | 'ADMIN';
export type ProductStatus = 'DRAFT' | 'ON_SALE' | 'OFF_SALE';
export type CommonStatus = 'ENABLED' | 'DISABLED';

export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  requestId?: string;
}

export interface PageResult<T> {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
}

export interface User {
  id: string;
  username: string;
  nickname: string;
  roles: Role[];
}

export interface Category {
  id: string;
  parentId?: string;
  name: string;
  level: number;
  sortOrder: number;
  status: CommonStatus;
}

export interface Brand {
  id: string;
  name: string;
  logoUrl?: string;
  description?: string;
  sortOrder: number;
  status: CommonStatus;
}

export interface Product {
  id: string;
  productNo: string;
  name: string;
  subtitle?: string;
  category: Category;
  brand?: Brand;
  salePrice: number;
  originalPrice?: number;
  stock: number;
  mainImageUrl: string;
  imageUrlsJson?: string;
  description: string;
  keywords?: string;
  scenarios?: string;
  audiences?: string;
  specificationJson?: string;
  status: ProductStatus;
  publishedAt?: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProductSearch {
  keyword?: string;
  categoryId?: string;
  brandId?: string;
  priceMin?: number;
  priceMax?: number;
  inStock?: boolean;
  status?: ProductStatus;
  sort?: 'RELEVANCE' | 'PRICE_ASC' | 'PRICE_DESC' | 'NEWEST' | 'POPULARITY' | 'HOT';
  page?: number;
  pageSize?: number;
}

export interface ProductInput {
  productNo: string;
  name: string;
  subtitle?: string;
  categoryId: string;
  brandId?: string;
  salePrice: number;
  originalPrice?: number;
  stock: number;
  mainImageUrl: string;
  imageUrlsJson?: string;
  description: string;
  keywords?: string;
  scenarios?: string;
  audiences?: string;
  specificationJson?: string;
  status: ProductStatus;
  version?: number;
}

export interface CartItem {
  id: string;
  product: Product;
  quantity: number;
  selected: boolean;
  available: boolean;
  subtotal: number;
}

export interface Cart {
  items: CartItem[];
  itemCount: number;
  selectedCount: number;
  selectedTotal: number;
}

export interface Favorite {
  id: string;
  createdAt: string;
  product: Product;
}

export class AppError extends Error {
  constructor(public code: string, message: string, public requestId?: string, public status?: number) {
    super(message);
  }
}

let refreshPromise: Promise<void> | null = null;

function createClient(baseURL: string): AxiosInstance {
  const client = axios.create({ baseURL, withCredentials: true });
  client.interceptors.response.use(
    (response) => response,
    async (error: AxiosError<ApiResponse<unknown>>) => {
      const config = error.config as (typeof error.config & { _retried?: boolean });
      const isAuthCall = config?.url?.includes('/auth/login') || config?.url?.includes('/auth/refresh');
      if (error.response?.status === 401 && config && !config._retried && !isAuthCall) {
        config._retried = true;
        refreshPromise ??= axios.post(`${import.meta.env.VITE_API_BASE_URL || '/api/v1'}/auth/refresh`, undefined,
          { withCredentials: true }).then(() => undefined).finally(() => { refreshPromise = null; });
        try {
          await refreshPromise;
          return client.request(config);
        } catch {
          window.dispatchEvent(new CustomEvent('auth:expired'));
        }
      }
      const body = error.response?.data;
      throw new AppError(body?.code || 'NETWORK_ERROR', body?.message || '网络连接失败，请稍后重试',
        body?.requestId, error.response?.status);
    },
  );
  return client;
}

export const http = createClient(import.meta.env.VITE_API_BASE_URL || '/api/v1');
export const httpV2 = createClient(import.meta.env.VITE_API_V2_BASE_URL || '/api/v2');
const data = async <T>(promise: Promise<{ data: ApiResponse<T> }>) => (await promise).data.data;

export const api = {
  auth: {
    me: () => data<User>(http.get('/auth/me')),
    login: (body: { account: string; password: string }) => data<User>(http.post('/auth/login', body)),
    register: (body: { username: string; phone?: string; email?: string; password: string }) => data<User>(http.post('/auth/register', body)),
    logout: () => data<void>(http.post('/auth/logout')),
  },
  catalog: {
    products: (params: ProductSearch = {}) => data<PageResult<Product>>(http.get('/products', { params })),
    product: (id: string) => data<Product>(http.get(`/products/${id}`)),
    categories: () => data<Category[]>(http.get('/categories')),
    brands: () => data<Brand[]>(http.get('/brands')),
    hot: (limit = 8) => data<Product[]>(http.get('/products/hot', { params: { limit } })),
  },
  favorites: {
    list: (page = 1) => data<PageResult<Favorite>>(http.get('/favorites', { params: { page } })),
    status: (productId: string) => data<{ favorite: boolean }>(http.get(`/favorites/${productId}/status`)),
    add: (productId: string) => data<void>(http.put(`/favorites/${productId}`)),
    remove: (productId: string) => data<void>(http.delete(`/favorites/${productId}`)),
  },
  cart: {
    get: () => data<Cart>(http.get('/cart')),
    add: (productId: string, quantity = 1) => data<Cart>(http.post('/cart/items', { productId, quantity })),
    update: (itemId: string, body: { quantity?: number; selected?: boolean }) => data<Cart>(http.patch(`/cart/items/${itemId}`, body)),
    remove: (itemId: string) => data<Cart>(http.delete(`/cart/items/${itemId}`)),
    selectAll: (selected: boolean) => data<Cart>(http.patch('/cart/items/selection', { selected })),
  },
  admin: {
    products: (params: ProductSearch = {}) => data<PageResult<Product>>(http.get('/admin/products', { params })),
    product: (id: string) => data<Product>(http.get(`/admin/products/${id}`)),
    createProduct: (body: ProductInput) => data<Product>(http.post('/admin/products', body)),
    updateProduct: (id: string, body: ProductInput) => data<Product>(http.put(`/admin/products/${id}`, body)),
    status: (id: string, status: ProductStatus, version: number) => data<Product>(http.patch(`/admin/products/${id}/status`, { status, version })),
    deleteProduct: (id: string, version: number) => data<void>(http.delete(`/admin/products/${id}`, { params: { version } })),
    categories: () => data<Category[]>(http.get('/admin/categories')),
    saveCategory: (id: string | undefined, body: Omit<Category, 'id' | 'level'>) =>
      data<Category>(id ? http.put(`/admin/categories/${id}`, body) : http.post('/admin/categories', body)),
    deleteCategory: (id: string) => data<void>(http.delete(`/admin/categories/${id}`)),
    brands: () => data<Brand[]>(http.get('/admin/brands')),
    saveBrand: (id: string | undefined, body: Omit<Brand, 'id'>) =>
      data<Brand>(id ? http.put(`/admin/brands/${id}`, body) : http.post('/admin/brands', body)),
    deleteBrand: (id: string) => data<void>(http.delete(`/admin/brands/${id}`)),
  },
};

export interface FacetItem { id: string; name: string; count: number }
export interface SearchV2Response extends PageResult<Product> {
  tookMs: number;
  degraded: boolean;
  facets: { categories: FacetItem[]; brands: FacetItem[] };
}
export interface SearchSuggestion { type: 'PRODUCT' | 'CATEGORY' | 'BRAND'; label: string; id: string }
export interface HotProductItem { rank: number; hotLabel: string; product: Product }
export interface HotProductResponse { items: HotProductItem[]; calculatedAt: string; degraded: boolean }
export interface BrowsingHistoryItem { id: string; product: Product; viewCount: number; lastViewedAt: string }

export const apiV2 = {
  search: {
    products: (params: ProductSearch = {}) => data<SearchV2Response>(httpV2.get('/products/search', { params })),
    suggestions: (keyword: string, limit = 8) => data<SearchSuggestion[]>(httpV2.get('/products/search/suggestions', { params: { keyword, limit } })),
    batchSummary: (productIds: string[]) => data<Product[]>(httpV2.post('/products:batch-summary', { productIds })),
  },
  hot: (categoryId?: string, limit = 8) => data<HotProductResponse>(httpV2.get('/products/hot', { params: { categoryId, limit } })),
  history: {
    record: (productId: string, clientViewId: string, anonymousId: string) => data<void>(httpV2.post(`/browsing-history/${productId}`,
      { clientViewId }, { headers: { 'X-Anonymous-Id': anonymousId } })),
    list: (page = 1, pageSize = 20) => data<PageResult<BrowsingHistoryItem>>(httpV2.get('/browsing-history', { params: { page, pageSize } })),
    remove: (productId: string) => data<void>(httpV2.delete(`/browsing-history/${productId}`)),
    clear: () => data<void>(httpV2.delete('/browsing-history')),
    merge: (items: { productId: string; lastViewedAt: string }[]) => data<void>(httpV2.post('/browsing-history:merge', { items })),
  },
};

export const formatPrice = (value: number | string) => `¥${Number(value).toFixed(2)}`;
