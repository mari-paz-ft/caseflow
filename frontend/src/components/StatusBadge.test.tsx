import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StatusBadge } from './StatusBadge'

describe('StatusBadge', () => {
  it('renders the localized label for a case status', () => {
    render(<StatusBadge status="APROVADA" />)

    expect(screen.getByText('Aprovada')).toBeTruthy()
  })
})
