import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import ProbabilityGauge from './ProbabilityGauge';
import RiskBadge from './RiskBadge';
import ShapChart from './ShapChart';
import { ErrorState } from './StatusViews';

describe('RiskBadge', () => {
  it.each([
    ['LOW', 'Low risk'],
    ['MODERATE', 'Moderate risk'],
    ['HIGH', 'High risk'],
  ] as const)('renders the %s band', (band, label) => {
    render(<RiskBadge band={band} />);
    const chip = screen.getByText(label).closest('[data-band]');
    expect(chip).toHaveAttribute('data-band', band);
  });
});

describe('ProbabilityGauge', () => {
  it('shows the probability as a percentage', () => {
    render(<ProbabilityGauge probability={0.8731} />);
    expect(screen.getByTestId('gauge-value')).toHaveTextContent('87.3%');
    expect(screen.getByRole('img')).toHaveAttribute('aria-label', 'Malignancy probability 87.3%');
  });

  it('clamps values outside 0 to 1', () => {
    render(<ProbabilityGauge probability={1.4} />);
    expect(screen.getByTestId('gauge-value')).toHaveTextContent('100.0%');
  });
});

describe('ShapChart', () => {
  it('renders the explanation legend', () => {
    render(
      <ShapChart
        contributions={[
          { feature: 'worst_radius', value: 25.1, shapValue: 1.2, direction: 'increases_risk' },
          { feature: 'mean_texture', value: 14.3, shapValue: -0.4, direction: 'decreases_risk' },
        ]}
        height={200}
      />,
    );
    expect(screen.getByText(/Red bars push the prediction towards malignant/)).toBeInTheDocument();
  });
});

describe('ErrorState', () => {
  it('shows the message and a retry button', () => {
    let retried = false;
    render(<ErrorState message="Server down" onRetry={() => (retried = true)} />);
    expect(screen.getByText('Server down')).toBeInTheDocument();
    screen.getByRole('button', { name: 'Retry' }).click();
    expect(retried).toBe(true);
  });
});
