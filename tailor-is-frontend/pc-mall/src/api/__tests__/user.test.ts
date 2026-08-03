import { describe, it, expect, vi, beforeEach } from 'vitest';

/**
 * 用户中心 API 单元测试
 * 重点回归：getFavorites / getPoints 须支持 { showError?: boolean } 透传，
 * 使后端不可用时 ProfileView 可静默降级到空列表/0积分，不弹"系统错误"/"系统繁忙"全局提示。
 */
vi.mock('../request', () => ({
    default: vi.fn(),
}));

import request from '../request';
import {
    updateProfile,
    updatePassword,
    getFavorites,
    getPoints,
    getPointsBalance,
    getPointsHistory,
    addFavorite,
    removeFavorite,
    checkFavorite,
    getUserInfo,
} from '../user';

describe('User API', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('updateProfile', () => {
        it('应提交资料并透传 showError', async () => {
            (request as vi.Mock).mockResolvedValue({ id: 1, nickname: 'new' });
            await updateProfile({ nickname: 'new' }, { showError: false });
            expect(request).toHaveBeenCalledWith({
                url: '/api/v1/user/profile',
                method: 'put',
                data: { nickname: 'new' },
                showError: false,
            });
        });
    });

    describe('updatePassword', () => {
        it('应提交新旧密码并透传 showError', async () => {
            (request as vi.Mock).mockResolvedValue(true);
            await updatePassword('old', 'new', { showError: false });
            expect(request).toHaveBeenCalledWith({
                url: '/api/v1/user/password',
                method: 'put',
                data: { oldPassword: 'old', newPassword: 'new' },
                showError: false,
            });
        });
    });

    describe('getFavorites', () => {
        it('应返回分页收藏列表', async () => {
            const mockPage = { records: [{ id: 1 }], total: 1, current: 1, size: 10 };
            (request as vi.Mock).mockResolvedValue(mockPage);
            const result = await getFavorites();
            expect(result).toEqual(mockPage);
            expect(request).toHaveBeenCalledWith({
                url: '/api/v1/favorite/list',
                method: 'get',
                showError: undefined,
            });
        });

        /** 回归：后端异常时静默降级，不弹"系统繁忙" */
        it('须将 showError:false 透传给 request（防误弹窗回归）', async () => {
            (request as vi.Mock).mockResolvedValue({ records: [], total: 0 });
            await getFavorites({ showError: false });
            expect(request).toHaveBeenCalledWith({
                url: '/api/v1/favorite/list',
                method: 'get',
                showError: false,
            });
        });
    });

    describe('getPointsBalance', () => {
        it('应请求 marketing 积分余额端点', async () => {
            (request as vi.Mock).mockResolvedValue(880);
            const result = await getPointsBalance();
            expect(result).toBe(880);
            expect(request).toHaveBeenCalledWith({
                url: '/api/v1/marketing/points/balance',
                method: 'get',
                showError: undefined,
            });
        });

        it('须透传 showError:false', async () => {
            (request as vi.Mock).mockResolvedValue(0);
            await getPointsBalance({ showError: false });
            expect(request).toHaveBeenCalledWith({
                url: '/api/v1/marketing/points/balance',
                method: 'get',
                showError: false,
            });
        });
    });

    describe('getPointsHistory', () => {
        it('应请求 marketing 积分记录端点并带分页参数', async () => {
            const mockPage = {
                records: [{ id: 1, pointsChange: 100, changeType: 1, description: '签到', createTime: '2026-08-02' }],
                total: 1,
            };
            (request as vi.Mock).mockResolvedValue(mockPage);
            const result = await getPointsHistory();
            expect(result.records.length).toBe(1);
            expect(request).toHaveBeenCalledWith({
                url: '/api/v1/marketing/points/history',
                method: 'get',
                params: { pageNum: 1, pageSize: 20 },
                showError: undefined,
            });
        });
    });

    describe('getPoints', () => {
        /** 回归：聚合 balance + history 两个后端端点，字段对齐 PointsRecord 实体 */
        it('应聚合 balance 与 history 两个端点', async () => {
            (request as vi.Mock)
                .mockResolvedValueOnce(880) // balance
                .mockResolvedValueOnce({ // history
                    records: [{ id: 1, pointsChange: 100, changeType: 1, description: '签到', createTime: '2026-08-02' }],
                    total: 1,
                });
            const result = await getPoints();
            expect(result.points).toBe(880);
            expect(result.records.length).toBe(1);
            expect(request).toHaveBeenCalledTimes(2);
            expect(request).toHaveBeenNthCalledWith(1, {
                url: '/api/v1/marketing/points/balance',
                method: 'get',
                showError: undefined,
            });
            expect(request).toHaveBeenNthCalledWith(2, {
                url: '/api/v1/marketing/points/history',
                method: 'get',
                params: { pageNum: 1, pageSize: 20 },
                showError: undefined,
            });
        });

        /** 回归：balance 失败时降级为 0 积分，history 仍正常返回 */
        it('balance 失败时降级为 0 积分', async () => {
            (request as vi.Mock)
                .mockRejectedValueOnce(new Error('fail')) // balance 失败
                .mockResolvedValueOnce({ records: [], total: 0 }); // history 正常
            const result = await getPoints({ showError: false });
            expect(result.points).toBe(0);
            expect(result.records).toEqual([]);
        });

        /** 回归：history 失败时降级为空记录，balance 仍正常返回 */
        it('history 失败时降级为空记录', async () => {
            (request as vi.Mock)
                .mockResolvedValueOnce(500) // balance 正常
                .mockRejectedValueOnce(new Error('fail')); // history 失败
            const result = await getPoints({ showError: false });
            expect(result.points).toBe(500);
            expect(result.records).toEqual([]);
        });

        /** 回归：须将 showError:false 透传给两个子请求（防误弹窗） */
        it('须将 showError:false 透传给 balance 与 history', async () => {
            (request as vi.Mock)
                .mockResolvedValueOnce(100)
                .mockResolvedValueOnce({ records: [], total: 0 });
            await getPoints({ showError: false });
            expect(request).toHaveBeenNthCalledWith(1, expect.objectContaining({ showError: false }));
            expect(request).toHaveBeenNthCalledWith(2, expect.objectContaining({ showError: false }));
        });
    });

    describe('收藏操作', () => {
        it('addFavorite 应POST并透传 showError', async () => {
            (request as vi.Mock).mockResolvedValue(true);
            await addFavorite(9, { showError: false });
            expect(request).toHaveBeenCalledWith({
                url: '/api/v1/favorite/9',
                method: 'post',
                showError: false,
            });
        });

        it('removeFavorite 应POST取消并透传 showError', async () => {
            (request as vi.Mock).mockResolvedValue(true);
            await removeFavorite(9, { showError: false });
            expect(request).toHaveBeenCalledWith({
                url: '/api/v1/favorite/cancel/9',
                method: 'post',
                showError: false,
            });
        });

        it('checkFavorite 应GET检测状态', async () => {
            (request as vi.Mock).mockResolvedValue(true);
            await checkFavorite(9);
            expect(request).toHaveBeenCalledWith({
                url: '/api/v1/favorite/check/9',
                method: 'get',
                showError: undefined,
            });
        });
    });

    describe('getUserInfo', () => {
        it('应获取当前用户信息', async () => {
            (request as vi.Mock).mockResolvedValue({ id: 1, username: 'u' });
            await getUserInfo();
            expect(request).toHaveBeenCalledWith({
                url: '/api/v1/user/info',
                method: 'get',
                showError: undefined,
            });
        });
    });
});
