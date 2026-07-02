import { EntityCodegen, EntitySchema } from '../EntityCodegen'

const sampleSchema: EntitySchema = {
  entityName: 'CustomerOrder',
  tableName: 'customer_orders',
  tenantScoped: true,
  fields: [
    { name: 'orderNumber', type: 'string', required: true, description: 'Unique order number' },
    { name: 'amount', type: 'number', required: true },
    { name: 'isPaid', type: 'boolean' },
    { name: 'placedAt', type: 'date' },
    { name: 'items', type: 'array', items: { type: 'string' } },
  ],
}

const codegen = new EntityCodegen()

describe('EntityCodegen', () => {
  describe('generateTypeScript', () => {
    it('produces interface with correct name', () => {
      const ts = codegen.generateTypeScript(sampleSchema)
      expect(ts).toContain('export interface CustomerOrder')
    })

    it('required fields have no optional marker', () => {
      const ts = codegen.generateTypeScript(sampleSchema)
      expect(ts).toContain('orderNumber: string')
      expect(ts).not.toContain('orderNumber?: string')
    })

    it('optional fields have optional marker', () => {
      const ts = codegen.generateTypeScript(sampleSchema)
      expect(ts).toContain('isPaid?: boolean')
    })

    it('array fields with items type generate correct type', () => {
      const ts = codegen.generateTypeScript(sampleSchema)
      expect(ts).toContain('items?: string[]')
    })

    it('includes id, tenantId, createdAt base fields', () => {
      const ts = codegen.generateTypeScript(sampleSchema)
      expect(ts).toContain('id: string')
      expect(ts).toContain('tenantId: string')
      expect(ts).toContain('createdAt: string')
    })
  })

  describe('generateJava', () => {
    it('produces class with correct name', () => {
      const java = codegen.generateJava(sampleSchema)
      expect(java).toContain('public class CustomerOrder')
    })

    it('uses table name from schema', () => {
      const java = codegen.generateJava(sampleSchema)
      expect(java).toContain('@Table(name = "customer_orders")')
    })

    it('maps string to Java String', () => {
      const java = codegen.generateJava(sampleSchema)
      expect(java).toContain('private String orderNumber')
    })

    it('maps number to Java Double', () => {
      const java = codegen.generateJava(sampleSchema)
      expect(java).toContain('private Double amount')
    })

    it('has Lombok annotations', () => {
      const java = codegen.generateJava(sampleSchema)
      expect(java).toContain('@Data')
      expect(java).toContain('@NoArgsConstructor')
    })
  })
})
