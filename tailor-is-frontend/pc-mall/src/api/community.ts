import request from './request';
import type { Post, Comment, PageResponse } from '@/types';

interface PostListParams {
  type?: number;
  current?: number;
  size?: number;
}

interface CreatePostData {
  title: string;
  content: string;
  images?: string[];
  videoUrl?: string;
  type: number;
}

interface CommentData {
  content: string;
  parentId?: number;
}

export function getPosts(params: PostListParams, options?: { showError?: boolean }) {
  return request<Record<string, unknown>, PageResponse<Post>>({
    url: '/api/v1/community/post',
    method: 'get',
    params,
    showError: options?.showError,
  });
}

export function getPostDetail(id: number) {
  return request<Record<string, unknown>, Post>({
    url: `/api/v1/community/post/detail/${id}`,
    method: 'get',
  });
}

export function createPost(data: CreatePostData) {
  return request<Record<string, unknown>, number>({
    url: '/api/v1/community/post/create',
    method: 'post',
    data,
  });
}

export function updatePost(id: number, data: CreatePostData) {
  return request<Record<string, unknown>, Post>({
    url: `/api/v1/community/post/update/${id}`,
    method: 'put',
    data,
  });
}

export function deletePost(id: number) {
  return request<Record<string, unknown>, boolean>({
    url: `/api/v1/community/post/delete/${id}`,
    method: 'delete',
  });
}

export function likePost(id: number) {
  return request<Record<string, unknown>, boolean>({
    url: '/api/v1/community/interaction/like',
    method: 'post',
    data: { targetType: 1, targetId: id },
  });
}

export function getPostComments(postId: number) {
  return request<Record<string, unknown>, PageResponse<Comment>>({
    url: `/api/v1/community/comment/list/${postId}`,
    method: 'get',
  });
}

export function commentPost(postId: number, data: CommentData) {
  return request<Record<string, unknown>, boolean>({
    url: '/api/v1/community/comment/create',
    method: 'post',
    data: { ...data, postId },
  });
}

export function deleteComment(commentId: number) {
  return request<Record<string, unknown>, boolean>({
    url: `/api/v1/community/comment/delete/${commentId}`,
    method: 'delete',
  });
}
