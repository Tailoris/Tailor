import { vi, describe, it, expect, beforeEach } from 'vitest';

/**
 * 学堂模块 API 单元测试
 * 重点回归：getCourseList 等接口须能将 showError:false 透传给 request，
 * 使后端 academy-service 不可用（503）时前端不弹"服务暂时不可用"全局提示，
 * 由 AcademyView 的 mock 降级逻辑静默接管。
 */
vi.mock('../request', () => ({
    default: vi.fn().mockResolvedValue([]),
}));

import request from '../request';
import { getCourseList, getCourseDetail, getCourseChapters } from '../academy';

describe('academy API', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('getCourseList 应使用正确 URL 与分页参数', async () => {
        const mockCourses = [
            { id: 1, title: '服装设计基础入门', categoryId: 1 },
            { id: 2, title: '服装打版技术详解', categoryId: 2 },
        ];
        (request as vi.Mock).mockResolvedValue(mockCourses);

        const result = await getCourseList({ pageNum: 2, pageSize: 20 });
        expect(result).toEqual(mockCourses);
        expect(request).toHaveBeenCalledWith({
            url: '/api/v1/academy/courses',
            method: 'get',
            params: { pageNum: 2, pageSize: 20 },
            showError: undefined,
        });
    });

    it('getCourseList 默认分页为 1/10', async () => {
        (request as vi.Mock).mockResolvedValue([]);
        await getCourseList();
        expect(request).toHaveBeenCalledWith({
            url: '/api/v1/academy/courses',
            method: 'get',
            params: { pageNum: 1, pageSize: 10 },
            showError: undefined,
        });
    });

    /**
     * 回归用例：后端 503 时静默降级
     * AcademyView 在 academy-service 不可用时会传 { showError: false }，
     * 必须透传至 request 配置，否则拦截器会弹出"服务暂时不可用"误导用户。
     */
    it('getCourseList 须将 showError:false 透传给 request（防 503 误弹窗回归）', async () => {
        (request as vi.Mock).mockResolvedValue([]);
        await getCourseList({ pageNum: 1, pageSize: 12 }, { showError: false });
        expect(request).toHaveBeenCalledWith({
            url: '/api/v1/academy/courses',
            method: 'get',
            params: { pageNum: 1, pageSize: 12 },
            showError: false,
        });
    });

    it('getCourseDetail 应拼接课程ID并透传 showError', async () => {
        const mockDetail = { id: 9, title: '高级定制工艺实操', categoryId: 3 };
        (request as vi.Mock).mockResolvedValue(mockDetail);

        const result = await getCourseDetail(9, { showError: false });
        expect(result).toEqual(mockDetail);
        expect(request).toHaveBeenCalledWith({
            url: '/api/v1/academy/courses/9',
            method: 'get',
            showError: false,
        });
    });

    it('getCourseChapters 应拼接章节路由并透传 showError', async () => {
        (request as vi.Mock).mockResolvedValue([]);
        await getCourseChapters(7, { showError: false });
        expect(request).toHaveBeenCalledWith({
            url: '/api/v1/academy/courses/7/chapters',
            method: 'get',
            showError: false,
        });
    });

    it('getCourseDetail / getCourseChapters 默认不抑制错误提示', async () => {
        (request as vi.Mock).mockResolvedValue({} as never);
        await getCourseDetail(1);
        expect(request).toHaveBeenLastCalledWith({
            url: '/api/v1/academy/courses/1',
            method: 'get',
            showError: undefined,
        });

        (request as vi.Mock).mockResolvedValue([] as never);
        await getCourseChapters(1);
        expect(request).toHaveBeenLastCalledWith({
            url: '/api/v1/academy/courses/1/chapters',
            method: 'get',
            showError: undefined,
        });
    });
});
