import request from './request'

/**
 * 课程实体
 */
export interface Course {
  id: number
  title: string
  description: string
  coverImage: string
  videoUrl: string
  categoryId: number
  duration: number
  status: number
  authorId: number
  createdAt: string
  updatedAt: string
}

/**
 * 课程章节实体
 */
export interface CourseChapter {
  id: number
  courseId: number
  title: string
  videoUrl: string
  duration: number
  sortOrder: number
}

/**
 * 获取课程列表
 * @param pageNum 页码
 * @param pageSize 每页数量
 */
export function getCourseList(params?: { pageNum?: number; pageSize?: number }) {
  return request<Record<string, unknown>, Course[]>({
    url: '/api/v1/academy/courses',
    method: 'get',
    params: {
      pageNum: params?.pageNum ?? 1,
      pageSize: params?.pageSize ?? 10
    }
  })
}

/**
 * 获取课程详情
 * @param id 课程ID
 */
export function getCourseDetail(id: number) {
  return request<Record<string, unknown>, Course>({
    url: `/api/v1/academy/courses/${id}`,
    method: 'get'
  })
}

/**
 * 获取课程章节列表
 * @param courseId 课程ID
 */
export function getCourseChapters(courseId: number) {
  return request<Record<string, unknown>, CourseChapter[]>({
    url: `/api/v1/academy/courses/${courseId}/chapters`,
    method: 'get'
  })
}