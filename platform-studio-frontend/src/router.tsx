import { createBrowserRouter, Navigate } from 'react-router-dom';
import Shell from './components/Layout/Shell';
import ArtifactsPage from './pages/ArtifactsPage';
import BpmnModelerPage from './components/BpmnModeler/BpmnModelerPage';
import DmnEditorPage from './components/DmnEditor/DmnEditorPage';
import FormDesignerPage from './components/FormDesigner/FormDesignerPage';
import DataModelerPage from './components/DataModeler/DataModelerPage';
import RoleManagerPage from './components/RoleManager/RoleManagerPage';
import PageBuilderPage from './components/PageBuilder/PageBuilderPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Shell />,
    children: [
      { index: true, element: <Navigate to="/artifacts" replace /> },
      { path: 'artifacts', element: <ArtifactsPage /> },
      { path: 'bpmn/:id?', element: <BpmnModelerPage /> },
      { path: 'dmn/:id?', element: <DmnEditorPage /> },
      { path: 'forms/:id?', element: <FormDesignerPage /> },
      { path: 'data', element: <DataModelerPage /> },
      { path: 'roles', element: <RoleManagerPage /> },
      { path: 'pages/:id?', element: <PageBuilderPage /> },
    ],
  },
]);
