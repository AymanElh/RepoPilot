import {inject, Service} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {API_BASE_URL} from '../config/api-config';
import {ProvidersResponse} from './api.models';

@Service()
export class ProvidersApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/ai/providers`;

  list(): Observable<ProvidersResponse> {
    return this.http.get<ProvidersResponse>(this.baseUrl);
  }
}
