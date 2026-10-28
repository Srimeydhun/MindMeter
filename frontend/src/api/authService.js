
import api from './axiosConfig';

export const loginUser = async (username, password, role) => {
  const response = await api.post('/auth/login', {
    username,
    password,
    role,
  });

  return response.data;
};

export const registerStudent = async (userData) => {
  const response = await api.post('/auth/register', userData);
  return response.data;
};

export const registerTeacher = async (userData) => {
  const response = await api.post('/auth/register/teacher', userData);
  return response.data;
};
