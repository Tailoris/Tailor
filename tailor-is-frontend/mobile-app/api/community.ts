import { get, post } from './request'
import type { PostInfo, CommentInfo, PaginationParams, PageResult } from './types'

interface CreatePostRequest {
  title: string
  content: string
  images?: string[]
}

interface AddCommentRequest {
  content: string
  parentId?: number
}

export function getPosts(params?: PaginationParams) {
  return get<PageResult<PostInfo>>('community/posts', params)
}

export function getPostDetail(id: number) {
  return get<PostInfo>(`community/post/${id}`)
}

export function createPost(data: CreatePostRequest) {
  return post<void>('community/post', data)
}

export function likePost(id: number) {
  return post<void>(`community/post/like/${id}`)
}

export function getComments(postId: number, params?: PaginationParams) {
  return get<PageResult<CommentInfo>>(`community/post/${postId}/comments`, params)
}

export function addComment(postId: number, data: AddCommentRequest) {
  return post<CommentInfo>(`community/post/${postId}/comment`, data)
}
