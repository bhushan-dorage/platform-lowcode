import axios from 'axios'
import keycloak from '../keycloak'

const client = axios.create({
  baseURL: '/api',
})

client.interceptors.request.use(async (config) => {
  if (keycloak.isTokenExpired(30)) {
    await keycloak.updateToken(30)
  }
  config.headers.Authorization = `Bearer ${keycloak.token}`
  return config
})

export default client
