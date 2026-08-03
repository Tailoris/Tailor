import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { nextTick } from 'vue';
import ElementPlus from 'element-plus';
import AdminLayout from './AdminLayout.vue';

/**
 * AdminLayout 左侧导航菜单悬停交互单元测试
 * 覆盖修复场景：
 *   1. 悬停一级菜单展开二级菜单（sub-menu-content-show 类应用）
 *   2. 离开收起
 *   3. 悬停另一菜单自动关闭前一个（unique-opened）
 *   4. 移动设备禁用悬停
 *   5. 点击展开/收起（@open/@close）不受影响
 */

const mockPush = vi.fn();

vi.mock('vue-router', () => ({
    useRoute: () => ({ path: '/dashboard', meta: {} }),
    useRouter: () => ({ push: mockPush }),
}));

vi.mock('@/api/search', () => ({
    globalSearch: vi.fn().mockResolvedValue({ users: [], products: [], orders: [], total: 0 }),
}));

vi.mock('@/api/notification', () => ({
    getUnreadCount: vi.fn().mockResolvedValue(0),
    getNotifications: vi.fn().mockResolvedValue({ records: [], total: 0 }),
    markAllNotificationsRead: vi.fn().mockResolvedValue(undefined),
    markNotificationRead: vi.fn().mockResolvedValue(undefined),
}));

// happy-dom 缺失的浏览器 API 兜底，避免 Element Plus 内部 observe 报错
class FakeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
}
global.ResizeObserver = FakeObserver as unknown as typeof ResizeObserver;
global.IntersectionObserver = FakeObserver as unknown as typeof IntersectionObserver;

function setViewportWidth(width: number) {
    Object.defineProperty(window, 'innerWidth', {
        value: width,
        writable: true,
        configurable: true,
    });
}

describe('AdminLayout 左侧导航菜单悬停交互', () => {
    let wrapper: ReturnType<typeof mount>;

    const mountComponent = async (opts: { mobile?: boolean } = {}) => {
        setViewportWidth(opts.mobile ? 500 : 1280);
        wrapper = mount(AdminLayout, {
            global: {
                plugins: [ElementPlus],
                stubs: {
                    RouterView: { template: '<div class="stub-router-view" />' },
                },
            },
        });
        await flushPromises();
        await nextTick();
        return wrapper;
    };

    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
        if (wrapper) {
            wrapper.unmount();
        }
        vi.restoreAllMocks();
    });

    it('悬停一级菜单应在延迟内展开对应二级菜单（添加 sub-menu-content-show 类）', async () => {
        const w = await mountComponent();
        const subMenus = w.findAll('li.el-sub-menu');
        expect(subMenus.length).toBeGreaterThan(0);

        // 第一个 el-sub-menu 为 "user"
        await subMenus[0].trigger('mouseenter');
        // 超过 showDelay(150ms) 触发展开
        vi.advanceTimersByTime(200);
        await nextTick();

        const contents = w.findAll('.sub-menu-content');
        expect(contents.length).toBeGreaterThan(0);
        expect(contents[0].classes()).toContain('sub-menu-content-show');
    });

    it('鼠标离开一级菜单应在延迟后收起二级菜单', async () => {
        const w = await mountComponent();
        const subMenus = w.findAll('li.el-sub-menu');

        await subMenus[0].trigger('mouseenter');
        vi.advanceTimersByTime(200);
        await nextTick();
        expect(w.findAll('.sub-menu-content')[0].classes()).toContain('sub-menu-content-show');

        await subMenus[0].trigger('mouseleave');
        // 超过 hideDelay(200ms) 触发收起
        vi.advanceTimersByTime(250);
        await nextTick();
        expect(w.findAll('.sub-menu-content')[0].classes()).not.toContain('sub-menu-content-show');
    });

    it('悬停另一一级菜单应自动关闭前一个（unique-opened 互斥）', async () => {
        const w = await mountComponent();
        const subMenus = w.findAll('li.el-sub-menu');
        expect(subMenus.length).toBeGreaterThanOrEqual(2);

        // 悬停 user（第一个）
        await subMenus[0].trigger('mouseenter');
        vi.advanceTimersByTime(200);
        await nextTick();
        expect(w.findAll('.sub-menu-content')[0].classes()).toContain('sub-menu-content-show');

        // 悬停 merchant（第二个）
        await subMenus[1].trigger('mouseenter');
        vi.advanceTimersByTime(200);
        await nextTick();

        // user 收起，merchant 展开
        expect(w.findAll('.sub-menu-content')[0].classes()).not.toContain('sub-menu-content-show');
        expect(w.findAll('.sub-menu-content')[1].classes()).toContain('sub-menu-content-show');
    });

    it('移动设备（窄屏 < 768px）不应响应鼠标悬停', async () => {
        const w = await mountComponent({ mobile: true });
        const subMenus = w.findAll('li.el-sub-menu');

        await subMenus[0].trigger('mouseenter');
        vi.advanceTimersByTime(300);
        await nextTick();

        expect(w.findAll('.sub-menu-content')[0].classes()).not.toContain('sub-menu-content-show');
    });

    it('点击展开/收起（@open/@close 事件）不受悬停修复影响', async () => {
        const w = await mountComponent();
        const menu = w.findComponent({ name: 'ElMenu' });
        const contents = w.findAll('.sub-menu-content');

        // 初始未展开
        expect(contents[0].classes()).not.toContain('sub-menu-content-show');

        // 模拟 Element Plus 点击展开触发 open 事件（签名: index, indexPath）
        menu.vm.$emit('open', 'user', ['user']);
        await nextTick();
        expect(w.findAll('.sub-menu-content')[0].classes()).toContain('sub-menu-content-show');

        // 模拟点击收起触发 close 事件
        menu.vm.$emit('close', 'user', ['user']);
        await nextTick();
        expect(w.findAll('.sub-menu-content')[0].classes()).not.toContain('sub-menu-content-show');
    });

    it('键盘 Enter/Space 应切换菜单展开状态（可访问性）', async () => {
        const w = await mountComponent();
        const titleWrappers = w.findAll('.menu-title-wrapper');
        expect(titleWrappers.length).toBeGreaterThan(0);

        // Enter 键展开
        await titleWrappers[0].trigger('keydown', { key: 'Enter' });
        await nextTick();
        expect(w.findAll('.sub-menu-content')[0].classes()).toContain('sub-menu-content-show');

        // 再次 Enter 键收起
        await titleWrappers[0].trigger('keydown', { key: 'Enter' });
        await nextTick();
        expect(w.findAll('.sub-menu-content')[0].classes()).not.toContain('sub-menu-content-show');
    });
});
