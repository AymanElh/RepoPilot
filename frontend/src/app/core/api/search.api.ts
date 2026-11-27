import {inject, Service} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {API_BASE_URL} from '../config/api-config';
import {CodeSearchResult} from './api.models';

@Service()
export class SearchApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/repositories`;

  search(owner: string, name: string, query: string): Observable<CodeSearchResult[]> {
    return this.http.get<CodeSearchResult[]>(
      `${this.baseUrl}/${encodeURIComponent(owner)}/${encodeURIComponent(name)}/search`,
      {params: new HttpParams().set('q', query)}
    );
  }
}
