import { Link } from 'react-router-dom';
import { useContext } from 'react';
import ThemeContext from '../theme/themeContext.js';

function BrandLogo({ className }) {
  const { theme } = useContext(ThemeContext);
  const logoSrc =
    theme === 'dark'
      ? '/images/mindmeter-logo-dark.png'
      : '/images/mindmeter-navbar-logo.png';

  return (
    <Link className={className} to="/" aria-label="MindMeter home">
      <img
        className="mindmeter-brand-image"
        src={logoSrc}
        alt="MindMeter"
      />
    </Link>
  );
}

export default BrandLogo;
