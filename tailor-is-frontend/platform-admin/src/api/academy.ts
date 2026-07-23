import request from './request';
import type { PageResult } from './role';

export interface CourseInfo {
  id: number;
  name: string;
  instructor: string;
  instructorId: number;
  category: string;
  level: 'beginner' | 'intermediate' | 'advanced' | 'master';
  duration: string;
  lessons: number;
  students: number;
  price: number;
  rating: number;
  status: 'pending' | 'published' | 'offline';
  description: string;
  coverImage: string;
  createTime: string;
  publishTime: string;
}

export interface InstructorInfo {
  id: number;
  name: string;
  title: string;
  avatar: string;
  courses: number;
  students: number;
  rating: number;
  experience: string;
  specialty: string[];
  status: 'pending' | 'verified' | 'disabled';
  description: string;
  createTime: string;
}

export interface AcademyQueryParams {
  keyword?: string;
  status?: string;
  category?: string;
  level?: string;
  current?: number;
  size?: number;
}

const mockCourses: CourseInfo[] = [
  {
    id: 1, name: '服装打版基础教程', instructor: '李明', instructorId: 401,
    category: '打版', level: 'beginner', duration: '3小时', lessons: 12, students: 1256,
    price: 0, rating: 4.8, status: 'published',
    description: '从零开始学习服装打版基础知识，适合新手入门',
    coverImage: '', createTime: '2026-07-15 10:00:00', publishTime: '2026-07-16 08:00:00',
  },
  {
    id: 2, name: 'AI设计工具入门', instructor: '张伟', instructorId: 402,
    category: '设计', level: 'intermediate', duration: '2小时', lessons: 8, students: 890,
    price: 99, rating: 4.5, status: 'pending',
    description: '学习如何使用AI工具辅助服装设计，提升设计效率',
    coverImage: '', createTime: '2026-07-20 14:00:00', publishTime: '',
  },
  {
    id: 3, name: '高级立裁技巧', instructor: '王芳', instructorId: 403,
    category: '打版', level: 'advanced', duration: '5小时', lessons: 20, students: 560,
    price: 299, rating: 4.9, status: 'published',
    description: '深入掌握高级立体裁剪技巧，适合有经验的打版师',
    coverImage: '', createTime: '2026-07-10 09:00:00', publishTime: '2026-07-12 08:00:00',
  },
  {
    id: 4, name: '大师级服装设计', instructor: '陈雪', instructorId: 404,
    category: '设计', level: 'master', duration: '8小时', lessons: 32, students: 320,
    price: 599, rating: 4.95, status: 'published',
    description: '行业大师亲授，涵盖从设计到成衣的全流程',
    coverImage: '', createTime: '2026-07-05 10:00:00', publishTime: '2026-07-08 08:00:00',
  },
];

const mockInstructors: InstructorInfo[] = [
  {
    id: 1, name: '李明', title: '高级版师', avatar: '',
    courses: 8, students: 5620, rating: 4.8, experience: '15年',
    specialty: ['打版', '立裁', '工艺'], status: 'verified',
    description: '拥有15年服装打版经验，曾服务于多家知名服装品牌',
    createTime: '2026-07-01 10:00:00',
  },
  {
    id: 2, name: '张伟', title: '资深设计师', avatar: '',
    courses: 5, students: 3280, rating: 4.6, experience: '12年',
    specialty: ['设计', 'AI'], status: 'pending',
    description: '资深服装设计师，擅长数字化设计工具应用',
    createTime: '2026-07-05 14:00:00',
  },
  {
    id: 3, name: '王芳', title: '立裁专家', avatar: '',
    courses: 12, students: 8560, rating: 4.9, experience: '20年',
    specialty: ['立裁', '打版', '教学'], status: 'verified',
    description: '行业知名立裁专家，出版多本服装专业教材',
    createTime: '2026-07-03 09:00:00',
  },
];

export async function listCourses(params: AcademyQueryParams): Promise<PageResult<CourseInfo>> {
  try {
    return await request<unknown, PageResult<CourseInfo>>({
      url: '/admin/academy/course',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockCourses];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(c =>
        c.name.toLowerCase().includes(kw) ||
        c.instructor.toLowerCase().includes(kw)
      );
    }
    if (params.status) {
      filtered = filtered.filter(c => c.status === params.status);
    }
    if (params.category) {
      filtered = filtered.filter(c => c.category === params.category);
    }
    if (params.level) {
      filtered = filtered.filter(c => c.level === params.level);
    }
    const total = filtered.length;
    const start = ((params.current || 1) - 1) * (params.size || 10);
    return {
      records: filtered.slice(start, start + (params.size || 10)),
      total,
      current: params.current || 1,
      size: params.size || 10,
    };
  }
}

export async function createCourse(data: Partial<CourseInfo>): Promise<CourseInfo> {
  try {
    return await request<unknown, CourseInfo>({
      url: '/admin/academy/course',
      method: 'post',
      data,
      showError: false,
    });
  } catch {
    return {
      id: Date.now(),
      ...data,
      students: 0,
      rating: 0,
      status: 'pending',
      createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
      publishTime: '',
    } as CourseInfo;
  }
}

export async function updateCourse(id: number, data: Partial<CourseInfo>): Promise<CourseInfo> {
  try {
    return await request<unknown, CourseInfo>({
      url: `/admin/academy/course/${id}`,
      method: 'put',
      data,
      showError: false,
    });
  } catch {
    const existing = mockCourses.find(c => c.id === id);
    return { ...existing!, ...data };
  }
}

export async function deleteCourse(id: number): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/academy/course/${id}`,
      method: 'delete',
      showError: false,
    });
  } catch {
    // Mock success
  }
}

export async function listInstructors(params: AcademyQueryParams): Promise<PageResult<InstructorInfo>> {
  try {
    return await request<unknown, PageResult<InstructorInfo>>({
      url: '/admin/academy/instructor',
      method: 'get',
      params,
      showError: false,
    });
  } catch {
    let filtered = [...mockInstructors];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(i =>
        i.name.toLowerCase().includes(kw) ||
        i.title.includes(kw)
      );
    }
    if (params.status) {
      filtered = filtered.filter(i => i.status === params.status);
    }
    const total = filtered.length;
    const start = ((params.current || 1) - 1) * (params.size || 10);
    return {
      records: filtered.slice(start, start + (params.size || 10)),
      total,
      current: params.current || 1,
      size: params.size || 10,
    };
  }
}

export async function createInstructor(data: Partial<InstructorInfo>): Promise<InstructorInfo> {
  try {
    return await request<unknown, InstructorInfo>({
      url: '/admin/academy/instructor',
      method: 'post',
      data,
      showError: false,
    });
  } catch {
    return {
      id: Date.now(),
      ...data,
      courses: 0,
      students: 0,
      rating: 0,
      status: 'pending',
      createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
    } as InstructorInfo;
  }
}

export async function updateInstructor(id: number, data: Partial<InstructorInfo>): Promise<InstructorInfo> {
  try {
    return await request<unknown, InstructorInfo>({
      url: `/admin/academy/instructor/${id}`,
      method: 'put',
      data,
      showError: false,
    });
  } catch {
    const existing = mockInstructors.find(i => i.id === id);
    return { ...existing!, ...data };
  }
}

export async function deleteInstructor(id: number): Promise<void> {
  try {
    await request<unknown, void>({
      url: `/admin/academy/instructor/${id}`,
      method: 'delete',
      showError: false,
    });
  } catch {
    // Mock success
  }
}