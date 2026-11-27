import {inject, Service} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {API_BASE_URL} from '../config/api-config';
import {ChatProvider, ChatRequest, ChatResponse} from './api.models';

@Service()
export class ChatApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/repositories`;

  ask(owner: string, name: string, question: string, provider?: ChatProvider): Observable<ChatResponse> {
    const body: ChatRequest = provider ? {question, provider} : {question};
    return this.http.post<ChatResponse>(
      `${this.baseUrl}/${encodeURIComponent(owner)}/${encodeURIComponent(name)}/chat`,
      body
    );
  }
}
