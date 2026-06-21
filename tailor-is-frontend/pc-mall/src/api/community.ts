import request from './request'
import type { Post, Comment, PageResponse } from '@/types'

interface PostListParams {
  type?: number
  current?: number
  size?: number
}

interface CreatePostData {
  title: string
  content: string
  images?: string[]
  videoUrl?: string
  type: number
}

interface CommentData {
  content: string
  parentId?: number
}

export function getPosts(params: PostListParams) {
  return request<Record<string, unknown>, PageResponse<Post>>({
    url: '/community/posts',
    method: 'get',
    params
  })
}

export function getPostDetail(id: number) {
  return request<Record<string, unknown>, Post>({
    url: `/community/posts/${id}`,
    method: 'get'
  })
}

export function createPost(data: CreatePostData) {
  return request<Record<string, unknown>, number>({
    url: '/community/posts',
    method: 'post',
    data
  })
}

export function likePost(id: number) {
  return request<Record<string, unknown>, boolean>({
    url: `/community/posts/${id}/like`,
    method: 'post'
  })
}

export function getPostComments(postId: number) {
  return request<Record<string, unknown>, Comment[]>({
    url: `/community/posts/${postId}/comments`,
    method: 'get'
  })
}

export function commentPost(postId: number, data: CommentData) {
  return request<Record<string, unknown>, boolean>({
    url: `/community/posts/${postId}/comments`,
    method: 'post',
    data
  })
}
