import {inject, Service} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {API_BASE_URL} from '../config/api-config';
import {IndexRepositoryRequest, Repository} from './api.models';

@Service()
export class RepositoriesApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/repositories`;

  index(url: string): Observable<Repository> {
    const body: IndexRepositoryRequest = {url};
    return this.http.post<Repository>(this.baseUrl, body);
  }

  list(): Observable<Repository[]> {
    return this.http.get<Repository[]>(this.baseUrl);
  }

  get(owner: string, name: string): Observable<Repository> {
    return this.http.get<Repository>(
      `${this.baseUrl}/${encodeURIComponent(owner)}/${encodeURIComponent(name)}`
    );
  }
}
