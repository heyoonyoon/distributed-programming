export type PolicyholderProfile = {
  name: string
  email: string
  phone: string
  address: string
  bankAccount: string
}

export type UpdateProfileRequest = {
  email: string
  phone: string
  address: string
  bankAccount: string
}
