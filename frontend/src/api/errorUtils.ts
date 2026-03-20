/**
 * Extract a human-readable error message from an Axios error.
 * Backend returns: { error: { code, message } }
 */
export function getErrorMessage(err: any, fallback = 'Something went wrong'): string {
  const code: string = err?.response?.data?.error?.code || ''
  const msg: string =
    err?.response?.data?.error?.message ||
    err?.response?.data?.message ||
    err?.message ||
    fallback

  // Map known error codes to friendly messages
  switch (code) {
    case 'EMAIL_ALREADY_EXISTS':
      return 'This email is already registered. Please sign in instead.'
    case 'PHONE_ALREADY_EXISTS':
      return 'This phone number is already registered. Use a different number or leave it blank.'
    case 'INVALID_CREDENTIALS':
      return 'Invalid email or password.'
    case 'ACCOUNT_SUSPENDED':
      return 'Your account has been suspended. Contact support.'
    case 'USER_SUSPENDED':
      return 'Your account is suspended. You cannot perform this action.'
    case 'VEHICLE_REQUIRED':
      return 'Please add your vehicle details before posting a trip.'
    case 'TOO_MANY_WAYPOINTS':
      return 'A trip can have at most 5 intermediate stops.'
    case 'TRIP_NOT_OPEN':
      return 'This trip is no longer open.'
    case 'SEATS_BELOW_CONFIRMED':
      return msg // already has the confirmed count in it
    case 'CANCELLATION_TOO_LATE':
      return 'Trips can only be cancelled more than 2 hours before departure.'
    case 'TOO_EARLY_TO_START':
      return 'You can only start the trip within 30 minutes of departure.'
    case 'NOT_AT_DESTINATION':
      return msg // has distance info
    case 'MAIL_NOT_CONFIGURED':
      return 'Email service is not set up on this server. Contact the administrator.'
    case 'MAIL_SEND_FAILED':
      return 'Could not send the reset email. Please check your email address and try again.'
    case 'SMS_NOT_CONFIGURED':
      return 'SMS service is not set up on this server. Contact the administrator.'
    case 'SMS_SEND_FAILED':
      return 'Could not send the OTP SMS. Please try again or use email reset.'
    case 'INVALID_RESET_TOKEN':
      return 'The reset token is invalid or has expired. Please request a new one.'
    case 'INVALID_OTP':
      return 'The OTP is incorrect or has expired. Please request a new one.'
    case 'INVALID_IDENTITY':
      return 'Email or full name does not match our records. Please check and try again.'
    case 'BOOKING_NOT_FOUND':
    case 'TRIP_NOT_FOUND':
    case 'USER_NOT_FOUND':
      return 'Not found. It may have been removed.'
    case 'CONFLICT':
      return 'This action conflicts with an existing record.'
    default:
      return msg
  }
}
