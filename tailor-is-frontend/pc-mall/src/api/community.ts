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
  return request<any, PageResponse<Post>>({
    url: '/community/posts',
    method: 'get',
    params
  })
}

export function getPostDetail(id: number) {
  return request<any, Post>({
    url: `/community/posts/${id}`,
    method: 'get'
  })
}

export function createPost(data: CreatePostData) {
  return request<any, number>({
    url: '/community/posts',
    method: 'post',
    data
  })
}

export function likePost(id: number) {
  return request<any, boolean>({
    url: `/community/posts/${id}/like`,
    method: 'post'
  })
}

export function getPostComments(postId: number) {
  return request<any, Comment[]>({
    url: `/community/posts/${postId}/comments`,
    method: 'get'
  })
}

export function commentPost(postId: number, data: CommentData) {
  return request<any, boolean>({
    url: `/community/posts/${postId}/comments`,
    method: 'post',
    data
  })
}
